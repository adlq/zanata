/*
 * Copyright 2012, Red Hat, Inc. and individual contributors as indicated by the
 * @author tags. See the copyright.txt file in the distribution for a full
 * listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package org.zanata.service.impl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.persistence.EntityManager;

import org.hibernate.HibernateException;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.TransactionPropagationType;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.core.Events;
import org.jboss.seam.security.management.JpaIdentityStore;
import org.jboss.seam.util.Work;
import org.zanata.common.ContentState;
import org.zanata.common.LocaleId;
import org.zanata.common.MergeType;
import org.zanata.common.util.ContentStateUtil;
import org.zanata.dao.DocumentDAO;
import org.zanata.dao.PersonDAO;
import org.zanata.dao.ProjectIterationDAO;
import org.zanata.dao.TextFlowDAO;
import org.zanata.dao.TextFlowTargetDAO;
import org.zanata.dao.TextFlowTargetHistoryDAO;
import org.zanata.exception.ConcurrentTranslationException;
import org.zanata.exception.ZanataServiceException;
import org.zanata.lock.Lock;
import org.zanata.model.HAccount;
import org.zanata.model.HDocument;
import org.zanata.model.HLocale;
import org.zanata.model.HPerson;
import org.zanata.model.HProjectIteration;
import org.zanata.model.HSimpleComment;
import org.zanata.model.HTextFlow;
import org.zanata.model.HTextFlowTarget;
import org.zanata.model.HTextFlowTargetHistory;
import org.zanata.process.MessagesProcessHandle;
import org.zanata.rest.dto.resource.TextFlowTarget;
import org.zanata.rest.dto.resource.TranslationsResource;
import org.zanata.rest.service.ResourceUtils;
import org.zanata.service.LocaleService;
import org.zanata.service.LockManagerService;
import org.zanata.service.TranslationStateCache;
import org.zanata.service.TranslationService;
import org.zanata.webtrans.shared.model.TransUnitId;
import org.zanata.webtrans.shared.model.TransUnitUpdateInfo;
import org.zanata.webtrans.shared.model.TransUnitUpdateRequest;
import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;

@Name("translationServiceImpl")
@AutoCreate
@Scope(ScopeType.STATELESS)
@Transactional
@Slf4j
public class TranslationServiceImpl implements TranslationService
{

   public final String TEXT_FLOW_TRANSLATED_EVENT = "org.zanata.event.HTextFlowTranslated";

   @In
   private EntityManager entityManager;

   @In
   private ProjectIterationDAO projectIterationDAO;

   @In
   private DocumentDAO documentDAO;

   @In
   private PersonDAO personDAO;

   @In
   private TextFlowDAO textFlowDAO;

   @In
   private TextFlowTargetDAO textFlowTargetDAO;

   @In
   private TextFlowTargetHistoryDAO textFlowTargetHistoryDAO;

   @In
   private ResourceUtils resourceUtils;

   @In
   private LocaleService localeServiceImpl;

   @In
   private LockManagerService lockManagerServiceImpl;

   @In(required = false, scope = ScopeType.EVENT)
   MessagesProcessHandle asynchronousProcessHandle;
   @In(value = JpaIdentityStore.AUTHENTICATED_USER, scope = ScopeType.SESSION, required = false)
   private HAccount authenticatedAccount;

   // TODO delete this?
   @Override
   public TranslationResult translate(LocaleId localeId, TransUnitUpdateRequest translateRequest) throws ConcurrentTranslationException
   {
      HTextFlow hTextFlow = entityManager.find(HTextFlow.class, translateRequest.getTransUnitId().getValue());
      HLocale hLocale = validateLocale(localeId, hTextFlow);
      HTextFlowTarget hTextFlowTarget = textFlowTargetDAO.getOrCreateTarget(hTextFlow, hLocale);

      if (translateRequest.getBaseTranslationVersion() != hTextFlowTarget.getVersionNum())
      {
         log.warn("translation failed for textflow {}: base versionNum {} does not match current versionNum {}", new Object[] { hTextFlow.getId(), translateRequest.getBaseTranslationVersion(), hTextFlowTarget.getVersionNum() });
         throw new ConcurrentTranslationException(MessageFormat.format("base translation version num {0} does not match current version num {1}, aborting", translateRequest.getBaseTranslationVersion(), hTextFlowTarget.getVersionNum()));
      }

      TranslationResultImpl result = new TranslationResultImpl();
      result.baseVersion = hTextFlowTarget.getVersionNum();
      result.baseContentState = hTextFlowTarget.getState();

      int nPlurals = getNumPlurals(hLocale, hTextFlow);
      translate(hTextFlowTarget, translateRequest.getNewContents(), translateRequest.getNewContentState(), nPlurals);

      result.translatedTextFlowTarget = hTextFlowTarget;
      result.isSuccess = true;
      return result;
   }

   @Override
   public List<TranslationResult> translate(LocaleId localeId, List<TransUnitUpdateRequest> translationRequests)
   {
      List<TranslationResult> results = new ArrayList<TranslationResult>();

      // avoid locale check if there is nothing to translate
      if (translationRequests.isEmpty())
      {
         return results;
      }

      // single locale check - assumes update requests are all from the same
      // project-iteration
      HTextFlow sampleHTextFlow = entityManager.find(HTextFlow.class, translationRequests.get(0).getTransUnitId().getValue());
      HLocale hLocale = validateLocale(localeId, sampleHTextFlow);
      for (TransUnitUpdateRequest request : translationRequests)
      {
         HTextFlow hTextFlow = entityManager.find(HTextFlow.class, request.getTransUnitId().getValue());
         HTextFlowTarget hTextFlowTarget = textFlowTargetDAO.getOrCreateTarget(hTextFlow, hLocale);
         if (request.hasTargetComment())
         {
            hTextFlowTarget.setComment(new HSimpleComment(request.getTargetComment()));
         }

         TranslationResultImpl result = new TranslationResultImpl();
         result.baseVersion = hTextFlowTarget.getVersionNum();
         result.baseContentState = hTextFlowTarget.getState();

         if (request.getBaseTranslationVersion() == hTextFlowTarget.getVersionNum())
         {
            try
            {
               int nPlurals = getNumPlurals(hLocale, hTextFlow);
               result.targetChanged = translate(hTextFlowTarget, request.getNewContents(), request.getNewContentState(), nPlurals);
               result.isSuccess = true;
            }
            catch (HibernateException e)
            {
               result.isSuccess = false;
               log.warn("HibernateException while translating");
            }
         }
         else
         {
            // concurrent edits not allowed
            log.warn("translation failed for textflow {}: base versionNum {} does not match current versionNum {}", new Object[] { hTextFlow.getId(), request.getBaseTranslationVersion(), hTextFlowTarget.getVersionNum() });
            result.isSuccess = false;
         }
         result.translatedTextFlowTarget = hTextFlowTarget;
         results.add(result);
      }

      return results;
   }

   /**
    * Generate a {@link HLocale} for the given localeId and check that
    * translations for this locale are permitted.
    * 
    * @param localeId
    * @param sampleHTextFlow used to determine the project-iteration
    * @return the valid hLocale
    * @throws ZanataServiceException if the locale is not enabled for the
    *            project-iteration or server
    */
   private HLocale validateLocale(LocaleId localeId, HTextFlow sampleHTextFlow) throws ZanataServiceException
   {
      HProjectIteration projectIteration = sampleHTextFlow.getDocument().getProjectIteration();
      String projectSlug = projectIteration.getProject().getSlug();
      return localeServiceImpl.validateLocaleByProjectIteration(localeId, projectSlug, projectIteration.getSlug());
   }

   /**
    * Sends out an event to signal that a Text Flow target has been translated
    */
   private void signalPostTranslateEvent( HTextFlowTarget hTextFlowTarget )
   {
      if( Events.exists() )
      {
         Events.instance().raiseTransactionSuccessEvent(TEXT_FLOW_TRANSLATED_EVENT, hTextFlowTarget);
      }
   }

   /**
    * This method contains all logic to be run immediately after a Text Flow Target has
    * been successfully translated.
    *
    * @param hTextFlowTarget The text flow target that has just been modified (translated).
    */
   @Observer(TEXT_FLOW_TRANSLATED_EVENT)
   public void postTranslate( HTextFlowTarget hTextFlowTarget )
   {
      HTextFlow textFlow = hTextFlowTarget.getTextFlow();

      // Update the Translation state cache
      TranslationStateCache translationStateCacheImpl =
            (TranslationStateCache) Component.getInstance("translationStateCacheImpl");
      translationStateCacheImpl.textFlowStateUpdated(textFlow.getId(),
            hTextFlowTarget.getLocale().getLocaleId(),
            hTextFlowTarget.getState());
   }

   private boolean translate(@Nonnull
   HTextFlowTarget hTextFlowTarget, @Nonnull
   List<String> contentsToSave, ContentState requestedState, int nPlurals)
   {
      boolean targetChanged = false;
      targetChanged |= setContentIfChanged(hTextFlowTarget, contentsToSave);
      targetChanged |= setContentStateIfChanged(requestedState, hTextFlowTarget, nPlurals);

      if (targetChanged || hTextFlowTarget.getVersionNum() == 0)
      {
         HTextFlow textFlow = hTextFlowTarget.getTextFlow();
         hTextFlowTarget.setVersionNum(hTextFlowTarget.getVersionNum() + 1);
         hTextFlowTarget.setTextFlowRevision(textFlow.getRevision());
         hTextFlowTarget.setLastModifiedBy(authenticatedAccount.getPerson());
         log.debug("last modified by :{}", authenticatedAccount.getPerson().getName());

         this.signalPostTranslateEvent(hTextFlowTarget);
      }

      // save the target histories
      entityManager.flush();

      return targetChanged;
   }

   /**
    * @return true if the content was changed, false otherwise
    */
   private boolean setContentIfChanged(@Nonnull
   HTextFlowTarget hTextFlowTarget, @Nonnull
   List<String> contentsToSave)
   {
      if (!contentsToSave.equals(hTextFlowTarget.getContents()))
      {
         hTextFlowTarget.setContents(contentsToSave);
         return true;
      }
      else
      {
         return false;
      }
   }

   /**
    * Check that requestedState is valid for the given content, adjust if
    * necessary and set the new state if it has changed.
    * 
    * @return true if the content state or contents list were updated, false
    *         otherwise
    * @see #adjustContentsAndState(org.zanata.model.HTextFlowTarget, int,
    *      java.util.List)
    */
   private boolean setContentStateIfChanged(@Nonnull
   ContentState requestedState, @Nonnull
   HTextFlowTarget target, int nPlurals)
   {
      boolean changed = false;
      ContentState previousState = target.getState();
      target.setState(requestedState);
      ArrayList<String> warnings = new ArrayList<String>();
      changed |= adjustContentsAndState(target, nPlurals, warnings);
      for (String warning : warnings)
      {
         log.warn(warning);
      }
      if (target.getState() != previousState)
      {
         changed = true;
      }
      return changed;
   }

   /**
    * Checks target state against its contents. If necessary, modifies target
    * state and generates a warning
    * 
    * @param target HTextFlowTarget to check/modify
    * @param nPlurals number of plurals for this locale for this message: use 1
    *           if message does not support plurals
    * @param warnings a warning string will be added if state is adjusted
    * @return true if and only if some state was changed
    */
   private static boolean adjustContentsAndState(@Nonnull
   HTextFlowTarget target, int nPlurals, @Nonnull
   List<String> warnings)
   {
      ContentState oldState = target.getState();
      String resId = target.getTextFlow().getResId();
      boolean contentsChanged = ensureContentsSize(target, nPlurals, resId, warnings);

      List<String> contents = target.getContents();
      target.setState(ContentStateUtil.determineState(oldState, contents, resId, warnings));
      boolean stateChanged = (oldState != target.getState());
      return contentsChanged || stateChanged;
   }

   /**
    * Ensures that target.contents has exactly legalSize elements
    * 
    * @param target HTextFlowTarget to check/modify
    * @param legalSize required number of contents
    * @param resId ID of target
    * @param warnings if elements were added or removed
    * @return
    */
   private static boolean ensureContentsSize(HTextFlowTarget target, int legalSize, String resId, @Nonnull
   List<String> warnings)
   {
      int contentsSize = target.getContents().size();
      if (contentsSize < legalSize)
      {
         String warning = "Should have " + legalSize + " contents; adding empty strings: TextFlowTarget "+resId+" with contents: " + target.getContents();
         warnings.add(warning);
         List<String> newContents = new ArrayList<String>(legalSize);
         newContents.addAll(target.getContents());
         while (newContents.size() < legalSize)
         {
            newContents.add("");
         }
         target.setContents(newContents);
         return true;
      }
      else if (contentsSize > legalSize)
      {
         String warning = "Should have " + legalSize + " contents; discarding extra strings: TextFlowTarget "+resId+" with contents: " + target.getContents();
         warnings.add(warning);
         List<String> newContents = new ArrayList<String>(legalSize);
         for (int i = 0; i < contentsSize; i++)
         {
            String content = target.getContents().get(i);
            newContents.add(content);
         }
         target.setContents(newContents);
         return true;
      }
      return false;
   }

   @Override
   // This will not run in a transaction. Instead, transactions are controlled within the method itself.
   @Transactional(TransactionPropagationType.NEVER)
   public void translateAllInDoc( String projectSlug, String iterationSlug, String docId, LocaleId locale,
                                  TranslationsResource translations, Set<String> extensions, MergeType mergeType,
                                  boolean lock)
   {
      // Lock this document for push
      Lock transLock = null;
      if( lock )
      {
         transLock = new Lock(projectSlug, iterationSlug, docId, locale, "push");
         lockManagerServiceImpl.attain(transLock);
      }

      try
      {
         this.translateAllInDoc(projectSlug, iterationSlug, docId, locale, translations, extensions, mergeType);
      }
      finally
      {
         if(lock)
         {
            lockManagerServiceImpl.release(transLock);
         }
      }
   }

   @Override
   public List<String> translateAllInDoc(final String projectSlug, final String iterationSlug, final String docId, final LocaleId locale,
                                          final TranslationsResource translations, final Set<String> extensions, final MergeType mergeType)
   {
      HProjectIteration hProjectIteration = projectIterationDAO.getBySlug(projectSlug, iterationSlug);
      if (hProjectIteration == null)
      {
         throw new ZanataServiceException("Version '" + iterationSlug + "' for project '" + projectSlug + "' ");
      }

      ResourceUtils.validateExtensions(extensions);

      log.debug("pass evaluate");
      final HDocument document = documentDAO.getByDocIdAndIteration(hProjectIteration, docId);
      if (document.isObsolete())
      {
         throw new ZanataServiceException("A document was not found.", 404);
      }

      log.debug("start put translations entity:{}", translations);

      boolean changed = false;

      final HLocale hLocale = localeServiceImpl.validateLocaleByProjectIteration(locale, projectSlug, iterationSlug);

      try
      {
         changed |=
         new Work<Boolean>()
         {
            @Override
            protected Boolean work() throws Exception
            {
               // handle extensions
               boolean changed =
                     resourceUtils.transferFromTranslationsResourceExtensions(translations.getExtensions(true), document, extensions, hLocale, mergeType);
               return changed;
            }
         }.workInTransaction();
      }
      catch (Exception e)
      {
         throw new ZanataServiceException("Error during translation.", 500, e);
      }

      // NB: removedTargets only applies for MergeType.IMPORT
      final Collection<HTextFlowTarget> removedTargets = new HashSet<HTextFlowTarget>();
      final List<String> warnings = new ArrayList<String>();

      if (mergeType == MergeType.IMPORT)
      {
         for (HTextFlow textFlow : document.getTextFlows())
         {
            HTextFlowTarget hTarget = textFlow.getTargets().get(hLocale.getId());
            if (hTarget != null)
            {
               removedTargets.add(hTarget);
            }
         }
      }

      // Break the target into batches
      List<List<TextFlowTarget>> batches = Lists.partition(translations.getTextFlowTargets(), BATCH_SIZE);

      for( final List<TextFlowTarget> batch : batches )
      {
         try
         {
            changed |=
            new Work<Boolean>()
            {
               @Override
               protected Boolean work() throws Exception
               {
                  boolean changed = false;

                  for (int i = 0; i < batch.size(); i++)
                  {
                     TextFlowTarget incomingTarget = batch.get(i);

                     String resId = incomingTarget.getResId();
                     HTextFlow textFlow = textFlowDAO.getById(document, resId);
                     if (textFlow == null)
                     {
                        // return warning for unknown resId to caller
                        String warning = "Could not find TextFlow for TextFlowTarget "+resId+" with contents: " + incomingTarget.getContents();
                        warnings.add(warning);
                        if( asynchronousProcessHandle != null )
                        {
                           asynchronousProcessHandle.addMessages(warning);
                        }
                        log.warn("skipping TextFlowTarget with unknown resId: {}", resId);
                     }
                     else
                     {
                        int nPlurals = getNumPlurals(hLocale, textFlow);
                        HTextFlowTarget hTarget = textFlowTargetDAO.getTextFlowTarget(textFlow, hLocale);
                        boolean targetChanged = false;
                        if (hTarget == null)
                        {
                           targetChanged = true;
                           log.debug("locale: {}", locale);
                           hTarget = new HTextFlowTarget(textFlow, hLocale);
                           List<String> contents = Collections.nCopies(nPlurals, "");
                           hTarget.setContents(contents);
                           hTarget.setVersionNum(0); // incremented when content is set
                           //textFlowTargetDAO.makePersistent(hTarget);
                           textFlow.getTargets().put(hLocale.getId(), hTarget);
                           targetChanged |= resourceUtils.transferFromTextFlowTarget(incomingTarget, hTarget);
                           targetChanged |= resourceUtils.transferFromTextFlowTargetExtensions(incomingTarget.getExtensions(true), hTarget, extensions);
                        }
                        else
                        {
                           switch (mergeType)
                           {
                              case AUTO:
                                 if (incomingTarget.getState() != ContentState.New)
                                 {
                                    if (hTarget.getState() == ContentState.New)
                                    {
                                       targetChanged |= resourceUtils.transferFromTextFlowTarget(incomingTarget, hTarget);
                                       targetChanged |= resourceUtils.transferFromTextFlowTargetExtensions(incomingTarget.getExtensions(true), hTarget, extensions);
                                    }
                                    else if (incomingTarget.getState() == ContentState.Approved)
                                    {
                                       List<String> incomingContents = incomingTarget.getContents();
                                       boolean oldContent = textFlowTargetHistoryDAO.findContentInHistory(hTarget, incomingContents);
                                       if (!oldContent)
                                       {
                                          targetChanged |= resourceUtils.transferFromTextFlowTarget(incomingTarget, hTarget);
                                          targetChanged |= resourceUtils.transferFromTextFlowTargetExtensions(incomingTarget.getExtensions(true), hTarget, extensions);
                                       }
                                    }
                                    else
                                    {
                                       // incomingTarget state = NeedReview
                                       // hTarget state != New

                                       // we don't overwrite the server's NeedReview or
                                       // Approved value (business rule)
                                    }
                                 }
                                 break;

                              case IMPORT:
                                 removedTargets.remove(hTarget);
                                 targetChanged |= resourceUtils.transferFromTextFlowTarget(incomingTarget, hTarget);
                                 targetChanged |= resourceUtils.transferFromTextFlowTargetExtensions(incomingTarget.getExtensions(true), hTarget, extensions);
                                 break;

                              default:
                                 throw new ZanataServiceException("unhandled merge type " + mergeType);
                           }
                        }
                        targetChanged |= adjustContentsAndState(hTarget, nPlurals, warnings);

                        // update translation information if applicable
                        if (targetChanged)
                        {
                           hTarget.setVersionNum(hTarget.getVersionNum() + 1);

                           changed = true;
                           if (incomingTarget.getTranslator() != null)
                           {
                              String email = incomingTarget.getTranslator().getEmail();
                              HPerson hPerson = personDAO.findByEmail(email);
                              if (hPerson == null)
                              {
                                 hPerson = new HPerson();
                                 hPerson.setEmail(email);
                                 hPerson.setName(incomingTarget.getTranslator().getName());
                                 personDAO.makePersistent(hPerson);
                              }
                              hTarget.setLastModifiedBy(hPerson);
                           }
                           else
                           {
                              hTarget.setLastModifiedBy(null);
                           }
                           textFlowTargetDAO.makePersistent(hTarget);
                        }

                        signalPostTranslateEvent(hTarget);
                     }

                     personDAO.flush();
                     textFlowTargetDAO.flush();
                     personDAO.clear();
                     textFlowTargetDAO.clear();
                     if( asynchronousProcessHandle != null )
                     {
                        asynchronousProcessHandle.incrementProgress(1);
                     }
                  }

                  return changed;
               }
            }.workInTransaction();
         }
         catch (Exception e)
         {
            throw new ZanataServiceException("Error during translation.", 500, e);
         }

      }

      if (changed || !removedTargets.isEmpty())
      {
         try
         {
            new Work<Void>()
            {
               @Override
               protected Void work() throws Exception
               {
                  for (HTextFlowTarget target : removedTargets)
                  {
                     target = textFlowTargetDAO.findById(target.getId(), true); // need to refresh from persistence
                     target.clear();
                  }
                  textFlowTargetDAO.flush();

                  documentDAO.flush();
                  return null;
               }
            }.workInTransaction();
         }
         catch (Exception e)
         {
            throw new ZanataServiceException("Error during translation.", 500, e);
         }
      }

      return warnings;
   }

   private int getNumPlurals(HLocale hLocale, HTextFlow textFlow)
   {
      int nPlurals;
      if (!textFlow.isPlural())
      {
         nPlurals = 1;
      }
      else
      {
         nPlurals = resourceUtils.getNumPlurals(textFlow.getDocument(), hLocale);
      }
      return nPlurals;
   }

   public static class TranslationResultImpl implements TranslationResult
   {
      private HTextFlowTarget translatedTextFlowTarget;
      private boolean isSuccess;
      private boolean targetChanged = false;
      private int baseVersion;
      private ContentState baseContentState;

      @Override
      public boolean isTranslationSuccessful()
      {
         return isSuccess;
      }

      @Override
      public boolean isTargetChanged()
      {
         return targetChanged;
      }

      @Override
      public HTextFlowTarget getTranslatedTextFlowTarget()
      {
         return translatedTextFlowTarget;
      }

      @Override
      public int getBaseVersionNum()
      {
         return baseVersion;
      }

      @Override
      public ContentState getBaseContentState()
      {
         return baseContentState;
      }

   }

   @Override
   public List<TranslationResult> revertTranslations(LocaleId localeId, List<TransUnitUpdateInfo> translationsToRevert)
   {
      List<TranslationResult> results = new ArrayList<TranslationResult>();
      List<TransUnitUpdateRequest> updateRequests = new ArrayList<TransUnitUpdateRequest>();
      if (!translationsToRevert.isEmpty())
      {

         HTextFlow sampleHTextFlow = entityManager.find(HTextFlow.class, translationsToRevert.get(0).getTransUnit().getId().getValue());
         HLocale hLocale = validateLocale(localeId, sampleHTextFlow);
         for (TransUnitUpdateInfo info : translationsToRevert)
         {
            if (!info.isSuccess() || !info.isTargetChanged())
            {
               continue;
            }

            TransUnitId tuId = info.getTransUnit().getId();
            HTextFlow hTextFlow = entityManager.find(HTextFlow.class, tuId.getValue());
            HTextFlowTarget hTextFlowTarget = textFlowTargetDAO.getOrCreateTarget(hTextFlow, hLocale);

            // check that version has not advanced
            // TODO probably also want to check that source has not been updated
            Integer versionNum = hTextFlowTarget.getVersionNum();
            log.debug("about to revert hTextFlowTarget version {} to TransUnit version {}", versionNum, info.getTransUnit().getVerNum());
            if (versionNum.equals(info.getTransUnit().getVerNum()))
            {
               // look up replaced version
               HTextFlowTargetHistory oldTarget = hTextFlowTarget.getHistory().get(info.getPreviousVersionNum());
               if (oldTarget != null)
               {
                  // generate request
                  List<String> oldContents = oldTarget.getContents();
                  ContentState oldState = oldTarget.getState();
                  TransUnitUpdateRequest request = new TransUnitUpdateRequest(tuId, oldContents, oldState, versionNum);
                  // add to list
                  updateRequests.add(request);
               }
               else
               {
                  log.info("got null previous target for tu with id {}, version {}. Assuming previous state is untranslated", hTextFlow.getId(), info.getPreviousVersionNum());
                  List<String> emptyContents = Lists.newArrayList();
                  for (int i = 0; i < hTextFlowTarget.getContents().size(); i++)
                  {
                     emptyContents.add("");
                  }
                  TransUnitUpdateRequest request = new TransUnitUpdateRequest(tuId, emptyContents, ContentState.New, versionNum);
                  updateRequests.add(request);
               }
            }
            else
            {
               log.info("attempt to revert target version {} for tu with id {}, but current version is {}. Not reverting.", new Object[] { info.getTransUnit().getVerNum(), tuId, versionNum });
               results.add(buildFailResult(hTextFlowTarget));
            }
         }
      }
      results.addAll(translate(localeId, updateRequests));
      return results;
   }

   /**
    * @param hTextFlowTarget
    * @return
    */
   private TranslationResultImpl buildFailResult(HTextFlowTarget hTextFlowTarget)
   {
      TranslationResultImpl result = new TranslationResultImpl();
      result.baseVersion = hTextFlowTarget.getVersionNum();
      result.baseContentState = hTextFlowTarget.getState();
      result.isSuccess = false;
      result.translatedTextFlowTarget = hTextFlowTarget;
      return result;
   }
}
