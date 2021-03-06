package org.zanata.webtrans.client.presenter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.customware.gwt.presenter.client.EventBus;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.zanata.common.ContentState;
import org.zanata.common.LocaleId;
import org.zanata.common.ProjectType;
import org.zanata.common.TransUnitCount;
import org.zanata.common.TransUnitWords;
import org.zanata.common.TranslationStats;
import org.zanata.webtrans.client.events.DocumentSelectionEvent;
import org.zanata.webtrans.client.events.DocumentStatsUpdatedEvent;
import org.zanata.webtrans.client.events.ProjectStatsUpdatedEvent;
import org.zanata.webtrans.client.events.TransUnitUpdatedEvent;
import org.zanata.webtrans.client.events.UserConfigChangeEvent;
import org.zanata.webtrans.client.events.WorkspaceContextUpdateEvent;
import org.zanata.webtrans.client.history.History;
import org.zanata.webtrans.client.history.HistoryToken;
import org.zanata.webtrans.client.resources.WebTransMessages;
import org.zanata.webtrans.client.rpc.CachingDispatchAsync;
import org.zanata.webtrans.client.service.UserOptionsService;
import org.zanata.webtrans.client.ui.DocumentNode;
import org.zanata.webtrans.client.ui.HasPager;
import org.zanata.webtrans.client.view.DocumentListDisplay;
import org.zanata.webtrans.shared.model.AuditInfo;
import org.zanata.webtrans.shared.model.DocumentId;
import org.zanata.webtrans.shared.model.DocumentInfo;
import org.zanata.webtrans.shared.model.ProjectIterationId;
import org.zanata.webtrans.shared.model.TransUnit;
import org.zanata.webtrans.shared.model.TransUnitUpdateInfo;
import org.zanata.webtrans.shared.model.UserWorkspaceContext;
import org.zanata.webtrans.shared.model.ValidationId;
import org.zanata.webtrans.shared.model.ValidationInfo;
import org.zanata.webtrans.shared.model.WorkspaceContext;
import org.zanata.webtrans.shared.model.WorkspaceId;
import org.zanata.webtrans.shared.rpc.HasWorkspaceContextUpdateData;
import org.zanata.webtrans.shared.rpc.ThemesOption;

import com.google.gwt.event.shared.GwtEvent;

@Test(groups = { "unit-tests" })
public class DocumentListPresenterTest
{
   // field for document list presenter under test
   private DocumentListPresenter documentListPresenter;

   // mocks for interacting classes
   @Mock
   private DocumentListDisplay mockDisplay;
   @Mock
   private EventBus mockEventBus;
   @Mock
   private History mockHistory;
   @Mock
   private WebTransMessages mockMessages;
   @Mock
   private UserWorkspaceContext mockUserWorkspaceContext;
   @Mock
   private WorkspaceContext mockWorkspaceContext;
   @Mock
   private UserOptionsService mockUserOptionsService;
   @Mock
   private CachingDispatchAsync mockDispatcher;
   @Mock
   private HasPager mockPager;

   private UserConfigHolder configHolder;

   // captured events and handlers used in several tests
   @Captor
   private ArgumentCaptor<GwtEvent> capturedEventBusEvent;
   @Captor
   private ArgumentCaptor<Integer> capturedPageSize;
   @Captor
   private ArgumentCaptor<String> capturedHistoryTokenString;
   @Captor
   private ArgumentCaptor<HistoryToken> capturedHistoryToken;

   private static final String TEST_BY_WORDS_MESSAGE = "By Words";
   private static final String TEST_BY_MESSAGE_MESSAGE = "By Message";

   private WorkspaceId workspaceId;

   @BeforeMethod
   public void beforeMethod()
   {
      MockitoAnnotations.initMocks(this);
      configHolder = new UserConfigHolder();
      when(mockUserOptionsService.getConfigHolder()).thenReturn(configHolder);
      documentListPresenter = new DocumentListPresenter(mockDisplay, mockEventBus, mockDispatcher, mockUserWorkspaceContext, mockMessages, mockHistory, mockUserOptionsService);
   
      workspaceId = new WorkspaceId(new ProjectIterationId("projectSlug", "iterationSlug", ProjectType.Podir), LocaleId.EN_US);

      when(mockUserWorkspaceContext.getWorkspaceContext()).thenReturn(mockWorkspaceContext);
      when(mockWorkspaceContext.getWorkspaceId()).thenReturn(workspaceId);

      when(mockMessages.projectTypeNotSet()).thenReturn("Project not set");
      when(mockMessages.downloadAllAsZipDescription()).thenReturn("Download all translation file");
      
      when(mockDisplay.getPageNavigation()).thenReturn(mockPager);
   }

   @Test
   public void onBind()
   {
      when(mockMessages.byWords()).thenReturn(TEST_BY_WORDS_MESSAGE);
      when(mockMessages.byMessage()).thenReturn(TEST_BY_MESSAGE_MESSAGE);

      documentListPresenter.onBind();

//      verify(mockDisplay).renderTable(isA(NoSelectionModel.class));
//      verify(mockDisplay).setStatsFilter("Words");
//      verify(mockDisplay).updatePageSize(UserConfigHolder.DEFAULT_DOC_LIST_PAGE_SIZE);
      verify(mockDisplay).setListener(documentListPresenter);
      verify(mockEventBus).addHandler(DocumentSelectionEvent.getType(), documentListPresenter);
      verify(mockEventBus).addHandler(TransUnitUpdatedEvent.getType(), documentListPresenter);
      verify(mockEventBus).addHandler(UserConfigChangeEvent.TYPE, documentListPresenter);
      verify(mockEventBus).addHandler(WorkspaceContextUpdateEvent.getType(), documentListPresenter);

      verify(mockDisplay).setEnableDownloadZip(documentListPresenter.isZipFileDownloadAllowed(workspaceId.getProjectIterationId().getProjectType()));
      verify(mockDisplay).setDownloadZipButtonTitle(isA(String.class));
   }

   @Test
   public void loadDocsIntoDataProvider()
   {
      documentListPresenter.bind();
      documentListPresenter.setDocuments(buildSampleDocumentArray());

      // right amount of docs
      assertThat("the data provider should have the same sized document list returned from the server", documentListPresenter.getSortedNodes().size(), is(3));

      ArrayList<DocumentInfo> expectedDocs = buildSampleDocumentArray();

      ArrayList<DocumentInfo> actualDocInfos = new ArrayList<DocumentInfo>();
      for (DocumentNode node : documentListPresenter.getSortedNodes())
      {
         assertThat("the data provider should have only documents that were returned from the server", node.getDocInfo(), isIn(expectedDocs));
         actualDocInfos.add(node.getDocInfo());
      }
      assertThat("the data provider should have all documents returned from the server", actualDocInfos, hasItems(expectedDocs.get(0), expectedDocs.get(1), expectedDocs.get(2)));
   }

   @Test
   public void generatesDocumentStatsOnTuUpdate()
   {
      ArrayList<String> sources = new ArrayList<String>();
      sources.add("this is the source");
      boolean plural = false;

      ArrayList<String> targets = new ArrayList<String>();
      targets.add("this is the target");

      TransUnit newTransUnit = TransUnit.Builder.newTransUnitBuilder().setId(12345L).setResId("resId").setLocaleId("es").setPlural(plural).setSources(sources).setSourceComment("this is the source comment").setTargets(targets).setStatus(ContentState.Approved).setLastModifiedBy("lastModifiedBy").setLastModifiedTime(new Date()).setMsgContext("msgContext").setRowIndex(1).setVerNum(1).build();
      TransUnitUpdateInfo updateInfo = new TransUnitUpdateInfo(true, true, new DocumentId(2222L, ""), newTransUnit, 3, 0, ContentState.NeedReview);
      TransUnitUpdatedEvent mockEvent = mock(TransUnitUpdatedEvent.class);

      when(mockEvent.getUpdateInfo()).thenReturn(updateInfo);

      ArrayList<DocumentInfo> documentInfos = buildSampleDocumentArray();
      TranslationStats stats = new TranslationStats();
      for (DocumentInfo documentInfo : documentInfos)
      {
         stats.add(documentInfo.getStats());
      }

      documentListPresenter.bind();
      documentListPresenter.setDocuments(documentInfos);
      documentListPresenter.setProjectStats(stats);
      documentListPresenter.onTransUnitUpdated(mockEvent);

      verify(mockEventBus, times(2)).fireEvent(capturedEventBusEvent.capture());

      DocumentStatsUpdatedEvent docStatsEvent = null;
      for (GwtEvent event : capturedEventBusEvent.getAllValues())
      {
         if (event.getAssociatedType().equals(DocumentStatsUpdatedEvent.getType()))
         {
            docStatsEvent = (DocumentStatsUpdatedEvent) event;
         }
      }

      assertThat("a document stats event should be fired when a TU update event occurs, not found", docStatsEvent, notNullValue());

      // document stats
      assertThat("document id in document stats event shoudl match updated TU document id", docStatsEvent.getDocId(), equalTo(new DocumentId(2222L, "")));

      // check actual counts (approved/fuzzy/untranslated)
      // default TUs: 1/2/3
      // approving 1 fuzzy, expect 2/1/3
      assertThat("document Approved TU count should increase by 1 when a TU is updated from NeedsReview to Approved", docStatsEvent.getNewStats().getUnitCount().getApproved(), is(2));
      assertThat("document NeedsReview TU count should decrease by 1 when a TU is updated from NeedsReview to Approved", docStatsEvent.getNewStats().getUnitCount().getNeedReview(), is(1));
      assertThat("document Untranslated TU count should remain the same when a TU is updated from NeedsReview to Approved", docStatsEvent.getNewStats().getUnitCount().getUntranslated(), is(3));

      // default words: 4/5/6
      // approving 3 fuzzy so expect 7/2/6
      assertThat("document Approved words should increase when TU changes to Approved", docStatsEvent.getNewStats().getWordCount().getApproved(), is(7));
      assertThat("document NeedsReview words should decrease when a TU changes from NeedsReview", docStatsEvent.getNewStats().getWordCount().getNeedReview(), is(2));
      assertThat("document Untranslated words should not change when TU changes between NeedsReview and Approved", docStatsEvent.getNewStats().getWordCount().getNeedReview(), is(2));
   }

   @Test
   public void generatesProjectStatsOnTuUpdate()
   {
      ArrayList<String> sources = new ArrayList<String>();
      sources.add("this is the source");
      boolean plural = false;

      ArrayList<String> targets = new ArrayList<String>();
      targets.add("this is the target");

      TransUnit newTransUnit = TransUnit.Builder.newTransUnitBuilder().setId(12345L).setResId("resId").setLocaleId("es").setPlural(plural).setSources(sources).setSourceComment("this is the source comment").setTargets(targets).setStatus(ContentState.Approved).setLastModifiedBy("lastModifiedBy").setLastModifiedTime(new Date()).setMsgContext("msgContext").setRowIndex(1).setVerNum(1).build();
      TransUnitUpdateInfo updateInfo = new TransUnitUpdateInfo(true, true, new DocumentId(2222L, ""), newTransUnit, 3, 0, ContentState.NeedReview);
      TransUnitUpdatedEvent mockEvent = mock(TransUnitUpdatedEvent.class);

      when(mockEvent.getUpdateInfo()).thenReturn(updateInfo);

      ArrayList<DocumentInfo> documentInfos = buildSampleDocumentArray();
      TranslationStats stats = new TranslationStats();
      for (DocumentInfo documentInfo : documentInfos)
      {
         stats.add(documentInfo.getStats());
      }

      documentListPresenter.bind();
      documentListPresenter.setDocuments(documentInfos);
      documentListPresenter.setProjectStats(stats);
      documentListPresenter.onTransUnitUpdated(mockEvent);

      verify(mockEventBus, times(2)).fireEvent(capturedEventBusEvent.capture());

      ProjectStatsUpdatedEvent projectStatsEvent = null;

      for (GwtEvent event : capturedEventBusEvent.getAllValues())
      {
         if (event.getAssociatedType().equals(ProjectStatsUpdatedEvent.getType()))
         {
            projectStatsEvent = (ProjectStatsUpdatedEvent) event;
         }
      }

      assertThat("a project stats event should be fired when a TU update event occurs, not found", projectStatsEvent, notNullValue());

      // default TUs: 3/6/9 (approved/fuzzy/untranslated)
      // approving 1 fuzzy, expect 4/5/9
      assertThat("project Approved TU count should increase by 1 when a TU changes to Approved status", projectStatsEvent.getProjectStats().getUnitCount().getApproved(), is(4));
      assertThat("project NeedsReview TU count should decrease by 1 when a TU changes from Approved status", projectStatsEvent.getProjectStats().getUnitCount().getNeedReview(), is(5));
      assertThat("project Untranslates TU count should not change when TU changes between NeedsReview and Approved", projectStatsEvent.getProjectStats().getUnitCount().getUntranslated(), is(9));

      // default words: 12/15/18
      // approving 3 fuzzy, expect 15/12/18
      assertThat("project Approved words should increase when TU changes to Approved", projectStatsEvent.getProjectStats().getWordCount().getApproved(), is(15));
      assertThat("project NeedsReview words should decrease when a TU changes from NeedsReview", projectStatsEvent.getProjectStats().getWordCount().getNeedReview(), is(12));
      assertThat("project Untranslated words should not change when TU changes between NeedsReview and Approved", projectStatsEvent.getProjectStats().getWordCount().getUntranslated(), is(18));
   }

   @Test
   public void filterTextUpdateGeneratesHistoryToken()
   {
      String filterText = "path/doc12";
      // these seem to persist beyond verify, so setting them up here is fine

      documentListPresenter.bind();
      documentListPresenter.fireFilterToken(filterText);
      documentListPresenter.fireExactSearchToken(false);

      verify(mockHistory).newItem(capturedHistoryTokenString.capture());

      HistoryToken capturedHistoryToken = HistoryToken.fromTokenString(capturedHistoryTokenString.getValue());
      assertThat("generated history token filter text should match the filter textbox", capturedHistoryToken.getDocFilterText(), is(filterText));
      assertThat("generated history token filter exact flag should match the exact match checkbox", capturedHistoryToken.getDocFilterExact(), is(false));
   }

   @Test
   public void checkExactSearchCheckboxGeneratesHistoryToken()
   {
      documentListPresenter.bind();

      // simulate checking 'exact search' checkbox
      documentListPresenter.fireExactSearchToken(true);

      verify(mockHistory).newItem(capturedHistoryTokenString.capture());

      HistoryToken exactSearchToken = new HistoryToken();
      exactSearchToken.setDocFilterExact(true);
      assertThat("checking the 'exact search' checkbox should be reflected in a new history token", capturedHistoryTokenString.getValue(), is(exactSearchToken.toTokenString()));
   }

   @Test
   public void uncheckExactSearchCheckboxGeneratesHistoryToken()
   {
      // history reflects checkbox already checked
      HistoryToken exactSearchToken = new HistoryToken();
      exactSearchToken.setDocFilterExact(true);

      when(mockHistory.getToken()).thenReturn(exactSearchToken.toTokenString());

      documentListPresenter.bind();
      documentListPresenter.fireExactSearchToken(false);

      verify(mockHistory).newItem(capturedHistoryTokenString.capture());

      HistoryToken inexactSearchToken = new HistoryToken();
      inexactSearchToken.setDocFilterExact(false);
      assertThat("unchecking the 'exact search' checkbox should be reflected in a new history token", capturedHistoryTokenString.getValue(), is(inexactSearchToken.toTokenString()));
   }

   // TODO tests for check and uncheck case sensitive check

   @Test
   public void documentSelectUpdatesHistoryToken()
   {
      HistoryToken documentPathToken = new HistoryToken();
      documentPathToken.setDocumentPath(null);
      when(mockHistory.getHistoryToken()).thenReturn(documentPathToken);

      documentListPresenter.bind();

      // simulate document click on second document
      DocumentInfo docInfo = new DocumentInfo(new DocumentId(2222L, ""), "doc122", "second/path/", LocaleId.EN_US, new TranslationStats(), new AuditInfo(new Date(), "Translator"), new HashMap<String, String>(), new AuditInfo(new Date(), "last translator"));
      documentListPresenter.fireDocumentSelection(docInfo);

      verify(mockHistory).newItem(capturedHistoryToken.capture());
      verify(mockUserWorkspaceContext).setSelectedDoc(docInfo);

      HistoryToken newToken = capturedHistoryToken.getValue();
      assertThat("path of selected document should be set in history token", newToken.getDocumentPath(), is("second/path/doc122"));
      assertThat("view in history token should change to individual document view when a new document is selected", newToken.getView(), is(MainView.Editor));
   }

   @Test
   public void exactSearchMatchesExactOnly()
   {
      // should match 1 of the 3 sample documents
      String filterText = "match/exact/filter";

      documentListPresenter.bind();
      documentListPresenter.setDocuments(buildSampleDocumentArray());
      documentListPresenter.updateFilterAndRun(filterText, true, false);

      verify(mockDisplay).updateFilter(false, true, filterText);

      // simulate firing history change event
      HistoryToken historyTokenWithExactFilter = new HistoryToken();
      historyTokenWithExactFilter.setDocFilterText(filterText);
      historyTokenWithExactFilter.setDocFilterExact(true);

      ArrayList<DocumentInfo> expectedDocs = buildSampleDocumentArray();
      expectedDocs.remove(2); // third doc does not match the filter
      expectedDocs.remove(0); // first doc does not match the filter
      ArrayList<DocumentInfo> actualDocInfos = new ArrayList<DocumentInfo>();
      for (DocumentNode node : documentListPresenter.getSortedNodes())
      {
         assertThat("the data provider should have only documents that exactly match the current filter", node.getDocInfo(), isIn(expectedDocs));
         actualDocInfos.add(node.getDocInfo());
      }
      assertThat("the data provider should have all documents that exactly match the filter", actualDocInfos, hasItems(expectedDocs.get(0)));
      assertThat("the data provider list should contain exactly the number of documents matching the filter", documentListPresenter.getSortedNodes().size(), is(1));
   }

   // TODO test case sensitivity option

   @Test
   public void commaSeparatedFilter()
   {
      // should match first and last of the 3 sample documents
      // multiple matching strings for third to check that there is no
      // duplication, also variable whitespace
      String filterText = " does/not, not/match ,no/filter ";

      documentListPresenter.bind();
      documentListPresenter.setDocuments(buildSampleDocumentArray());
      documentListPresenter.updateFilterAndRun(filterText, false, false);

      // simulate firing history change event
      HistoryToken historyTokenWithFilter = new HistoryToken();
      historyTokenWithFilter.setDocFilterText(filterText);

      verify(mockDisplay).updateFilter(false, false, filterText);

      ArrayList<DocumentInfo> expectedDocs = buildSampleDocumentArray();
      expectedDocs.remove(1); // second doc does not match any of the filter
                              // strings
      ArrayList<DocumentInfo> actualDocInfos = new ArrayList<DocumentInfo>();
      for (DocumentNode node : documentListPresenter.getSortedNodes())
      {
         assertThat("the data provider should have only documents that match the current filter", node.getDocInfo(), isIn(expectedDocs));
         actualDocInfos.add(node.getDocInfo());
      }
      assertThat("the data provider should have all documents that match the filter", actualDocInfos, hasItems(expectedDocs.get(0), expectedDocs.get(1)));
      assertThat("the data provider list should contain exactly the number of documents matching the filter", documentListPresenter.getSortedNodes().size(), is(2));
   }

   // TODO test case sensitive check updated from history

   // TODO test: update selected document when different from doc selection
   // event, as in DocumentListPresenter.setSelection()

   @Test
   public void getDocumentId()
   {
      documentListPresenter.bind();
      documentListPresenter.setDocuments(buildSampleDocumentArray());

      // third document from buildSampleDocumentArray()
      DocumentId docId = documentListPresenter.getDocumentId("does/not/match/exact/filter");
      assertThat(docId.getId(), is(3333L));

      // second document from buildSampleDocumentArray()
      docId = documentListPresenter.getDocumentId("match/exact/filter");
      assertThat(docId.getId(), is(2222L));
   }

   @Test
   public void getDocumentInfo()
   {
      documentListPresenter.bind();
      documentListPresenter.setDocuments(buildSampleDocumentArray());

      DocumentInfo docInfo = documentListPresenter.getDocumentInfo(new DocumentId(1111L, ""));
      assertThat(docInfo, is(equalTo(new DocumentInfo(new DocumentId(1111L, ""), "doc111", "first/path/", LocaleId.EN_US, new TranslationStats(), new AuditInfo(new Date(), "Translator"), new HashMap<String, String>(), new AuditInfo(new Date(), "last translator")))));

      docInfo = documentListPresenter.getDocumentInfo(new DocumentId(3333L, ""));
      assertThat(docInfo, is(equalTo(new DocumentInfo(new DocumentId(3333L, ""), "doc123", "third/path/", LocaleId.EN_US, new TranslationStats(), new AuditInfo(new Date(), "Translator"), new HashMap<String, String>(), new AuditInfo(new Date(), "last translator")))));
   }

   @Test
   public void onUserConfigChangedDocument()
   {
      UserConfigChangeEvent mockEvent = mock(UserConfigChangeEvent.class);
      when(mockEvent.getView()).thenReturn(MainView.Documents);

      documentListPresenter.onUserConfigChanged(mockEvent);

//      verify(mockDisplay).updatePageSize(UserConfigHolder.DEFAULT_DOC_LIST_PAGE_SIZE);
   }

   @Test
   public void onUserConfigChangedEditor()
   {
      UserConfigChangeEvent mockEvent = mock(UserConfigChangeEvent.class);
      when(mockEvent.getView()).thenReturn(MainView.Editor);

      documentListPresenter.onUserConfigChanged(mockEvent);

      verify(mockDisplay).setLayout(ThemesOption.THEMES_DEFAULT.name());

      verifyZeroInteractions(mockDisplay);
   }

   @Test
   public void onWorkspaceContextUpdated()
   {
      WorkspaceContextUpdateEvent event = new WorkspaceContextUpdateEvent(workplaceContextData(true, ProjectType.Gettext));
      documentListPresenter.onWorkspaceContextUpdated(event);

      verify(mockDisplay).setEnableDownloadZip(documentListPresenter.isZipFileDownloadAllowed(event.getProjectType()));
      verify(mockDisplay).setDownloadZipButtonTitle(isA(String.class));
   }

   private static HasWorkspaceContextUpdateData workplaceContextData(final boolean projectActive, final ProjectType projectType)
   {
      return new HasWorkspaceContextUpdateData()
      {
         @Override
         public boolean isProjectActive()
         {
            return projectActive;
         }

         @Override
         public ProjectType getProjectType()
         {
            return projectType;
         }

         @Override
         public Map<ValidationId, ValidationInfo> getValidationInfoList()
         {
            return null;
         }
      };
   }

   private ArrayList<DocumentInfo> buildSampleDocumentArray()
   {
      ArrayList<DocumentInfo> docList = new ArrayList<DocumentInfo>();

      TransUnitCount unitCount = new TransUnitCount(1, 2, 3);
      TransUnitWords wordCount = new TransUnitWords(4, 5, 6);

      DocumentInfo docInfo = new DocumentInfo(new DocumentId(1111L, ""), "matches", "no/filter", LocaleId.EN_US, new TranslationStats(unitCount, wordCount), new AuditInfo(new Date(), "Translator"), new HashMap<String, String>(), new AuditInfo(new Date(), "last translator"));
      docList.add(docInfo);

      docInfo = new DocumentInfo(new DocumentId(2222L, ""), "filter", "match/exact/", LocaleId.EN_US, new TranslationStats(unitCount, wordCount), new AuditInfo(new Date(), "Translator"), new HashMap<String, String>(), new AuditInfo(new Date(),"last translator"));
      docList.add(docInfo);

      docInfo = new DocumentInfo(new DocumentId(3333L, ""), "filter", "does/not/match/exact/", LocaleId.EN_US, new TranslationStats(unitCount, wordCount), new AuditInfo(new Date(), "Translator"), new HashMap<String, String>(), new AuditInfo(new Date(), "last translator"));
      docList.add(docInfo);

      return docList;
   }
}
