/*
 * Copyright 2010, Red Hat, Inc. and individual contributors as indicated by the
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
package org.zanata.webtrans.client.editor.table;

import java.util.ArrayList;
import java.util.List;

import org.zanata.webtrans.client.resources.NavigationMessages;
import org.zanata.webtrans.client.ui.HighlightingLabel;
import org.zanata.webtrans.shared.model.TransUnit;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.VerticalPanel;

public class SourcePanel extends Composite implements HasValue<TransUnit>, HasClickHandlers
{

   private final FlowPanel panel;
   private final VerticalPanel sourceLabelsPanel;
   private TransUnit value;
   private final List<HighlightingLabel> hightlightingLabelList;

   private List<HasValue<Boolean>> selectSourceButtonList;

   public SourcePanel()
   {
      panel = new FlowPanel();
      panel.setSize("100%", "100%");

      initWidget(panel);

      sourceLabelsPanel = new VerticalPanel();
      sourceLabelsPanel.addStyleName("sourceTable");

      hightlightingLabelList = new ArrayList<HighlightingLabel>();

      panel.add(sourceLabelsPanel);
   }

   public void updateData(TransUnit value, NavigationMessages messages)
   {
      selectSourceButtonList = new ArrayList<HasValue<Boolean>>();
      this.value = value;
      hightlightingLabelList.clear();
      sourceLabelsPanel.clear();

      for (String source : value.getSources())
      {
         HighlightingLabel hightlightingLabel = new HighlightingLabel(source);
         hightlightingLabel.setStylePrimaryName("TableEditorContent");
         hightlightingLabel.setTitle(messages.sourceCommentLabel() + value.getSourceComment());

         HorizontalPanel sourcePanel = new HorizontalPanel();
         sourcePanel.setSize("100%", "100%");
         sourcePanel.add(hightlightingLabel);
         sourcePanel.addStyleName("sourceRow");

         RadioButton selectButton = new RadioButton("selectSource");
         selectButton.setTitle(source);
         
         selectSourceButtonList.add(selectButton);

         sourcePanel.add(selectButton);
         sourcePanel.setCellHorizontalAlignment(selectButton, HasHorizontalAlignment.ALIGN_RIGHT);

         hightlightingLabelList.add(hightlightingLabel);
         sourceLabelsPanel.add(sourcePanel);
      }
   }

   public List<HasValue<Boolean>> getSelectSourceButtonList()
   {
      if (selectSourceButtonList == null)
      {
         selectSourceButtonList = new ArrayList<HasValue<Boolean>>();
      }
      return selectSourceButtonList;
   }

   @Override
   public TransUnit getValue()
   {
      return value;
   }

   @Override
   public void setValue(TransUnit value)
   {
      setValue(value, true);
   }

   @Override
   public void setValue(TransUnit value, boolean fireEvents)
   {
      if (this.value != value)
      {
         this.value = value;
         if (fireEvents)
         {
            ValueChangeEvent.fire(this, value);
         }
      }
   }

   @Override
   public HandlerRegistration addValueChangeHandler(ValueChangeHandler<TransUnit> handler)
   {
      return addHandler(handler, ValueChangeEvent.getType());
   }

   @Override
   public HandlerRegistration addClickHandler(ClickHandler handler)
   {
      return addHandler(handler, ClickEvent.getType());
   }
   
   public void highlightSearch(String search)
   {
      for (HighlightingLabel sourceLabel : hightlightingLabelList)
      {
         sourceLabel.highlightSearch(search);
      }
   }
}
