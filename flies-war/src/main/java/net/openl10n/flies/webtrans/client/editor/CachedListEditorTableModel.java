package net.openl10n.flies.webtrans.client.editor;

import net.openl10n.flies.webtrans.shared.model.TransUnit;

import org.gwt.mosaic.ui.client.table.CachedTableModel;

import com.google.inject.Inject;

public class CachedListEditorTableModel extends CachedTableModel<TransUnit>
{

   @Inject
   public CachedListEditorTableModel(ListEditorTableModel tableModel)
   {
      super(tableModel);
   }

}