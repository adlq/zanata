package org.zanata.webtrans.server.rpc;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.ActionException;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.zanata.common.TranslationStats;
import org.zanata.dao.DocumentDAO;
import org.zanata.dao.TextFlowTargetDAO;
import org.zanata.model.HTextFlowTarget;
import org.zanata.service.TranslationStateCache;
import org.zanata.service.impl.StatisticsServiceImpl;
import org.zanata.webtrans.server.ActionHandlerFor;
import org.zanata.webtrans.shared.model.AuditInfo;
import org.zanata.webtrans.shared.model.DocumentId;
import org.zanata.webtrans.shared.model.DocumentStatus;
import org.zanata.webtrans.shared.rpc.GetDocumentStats;
import org.zanata.webtrans.shared.rpc.GetDocumentStatsResult;

@Name("webtrans.gwt.GetDocumentStatsHandler")
@Scope(ScopeType.STATELESS)
@ActionHandlerFor(GetDocumentStats.class)
public class GetDocumentStatsHandler extends AbstractActionHandler<GetDocumentStats, GetDocumentStatsResult>
{
   @In
   private StatisticsServiceImpl statisticsServiceImpl;

   @In
   private TranslationStateCache translationStateCacheImpl;

   @In
   private TextFlowTargetDAO textFlowTargetDAO;

   @In
   private DocumentDAO documentDAO;

   @Override
   public GetDocumentStatsResult execute(GetDocumentStats action, ExecutionContext context) throws ActionException
   {
      Map<DocumentId, TranslationStats> statsMap = new HashMap<DocumentId, TranslationStats>();
      Map<DocumentId, AuditInfo> lastTranslatedMap = new HashMap<DocumentId, AuditInfo>();

      for (DocumentId documentId : action.getDocIds())
      {
         TranslationStats stats = statisticsServiceImpl.getDocStatistics(documentId.getId(), action.getWorkspaceId().getLocaleId());
         statsMap.put(documentId, stats);

         DocumentStatus docStat = translationStateCacheImpl.getDocStats(documentId.getId(), action.getWorkspaceId().getLocaleId());

         lastTranslatedMap.put(documentId, new AuditInfo(docStat.getLastTranslatedDate(), docStat.getLastTranslatedBy()));
      }
      return new GetDocumentStatsResult(statsMap, lastTranslatedMap);
   }

   @Override
   public void rollback(GetDocumentStats action, GetDocumentStatsResult result, ExecutionContext context) throws ActionException
   {

   }
}