package org.zanata.webtrans.client.presenter;

import org.zanata.webtrans.client.ui.TranslationHistoryDisplay;
import org.zanata.webtrans.shared.model.TransHistoryItem;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.inject.Singleton;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Singleton
public class TransHistorySelectionModel extends MultiSelectionModel<TransHistoryItem>
{
   public TransHistorySelectionModel()
   {
      super(TranslationHistoryDisplay.HISTORY_ITEM_PROVIDES_KEY);
   }
}
