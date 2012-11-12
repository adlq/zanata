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
package org.zanata.webtrans.client.presenter;

import org.zanata.common.ContentState;
import org.zanata.webtrans.shared.rpc.NavOption;
import com.google.common.base.Predicate;
import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.inject.Singleton;


@Singleton
public class UserConfigHolder
{
   public static final Predicate<ContentState> FUZZY_OR_NEW_PREDICATE = new Predicate<ContentState>()
   {
      @Override
      public boolean apply(ContentState contentState)
      {
         return contentState == ContentState.New || contentState == ContentState.NeedReview;
      }
   };
   public static final Predicate<ContentState> FUZZY_PREDICATE = new Predicate<ContentState>()
   {
      @Override
      public boolean apply(ContentState contentState)
      {
         return contentState == ContentState.NeedReview;
      }
   };
   public static final Predicate<ContentState> NEW_PREDICATE = new Predicate<ContentState>()
   {
      @Override
      public boolean apply(ContentState contentState)
      {
         return contentState == ContentState.New;
      }
   };
   
   private ConfigurationState state;

   public UserConfigHolder()
   {
      // default state
      state = new ConfigurationState();
      state.displayButtons = true;
      state.enterSavesApproved = false;
      state.pageSize = 25;
      state.navOption = NavOption.FUZZY_UNTRANSLATED;
      state.showError =false;
      state.useCodeMirrorEditor = true;
   }

   public boolean isEnterSavesApproved()
   {
      return state.isEnterSavesApproved();
   }

   protected void setEnterSavesApproved(boolean enterSavesApproved)
   {
      state = new ConfigurationState(state);
      state.enterSavesApproved = enterSavesApproved;
   }

   public boolean isDisplayButtons()
   {
      return state.isDisplayButtons();
   }

   protected void setDisplayButtons(boolean displayButtons)
   {
      state = new ConfigurationState(state);
      state.displayButtons = displayButtons;
   }

   protected void setNavOption(NavOption navOption)
   {
      state = new ConfigurationState(state);
      state.navOption = navOption;
   }

   public NavOption getNavOption()
   {
      return state.getNavOption();
   }

   public Predicate<ContentState> getContentStatePredicate()
   {
      if (state.getNavOption() == NavOption.FUZZY_UNTRANSLATED)
      {
         return FUZZY_OR_NEW_PREDICATE;
      }
      else if (state.getNavOption() == NavOption.FUZZY)
      {
         return FUZZY_PREDICATE;
      }
      else
      {
         return NEW_PREDICATE;
      }
   }

   public int getPageSize()
   {
      return state.getPageSize();
   }

   protected void setPageSize(int pageSize)
   {
      state = new ConfigurationState(state);
      state.pageSize = pageSize;
   }

   public ConfigurationState getState()
   {
      return state;
   }

   public boolean isShowError()
   {
      return state.isShowError();
   }

   public void setShowError(boolean showError)
   {
      state = new ConfigurationState(state);
      state.showError = showError;
   }

   public boolean isUseCodeMirrorEditor()
   {
      return state.isUseCodeMirrorEditor();
   }

   public void setUseCodeMirrorEditor(boolean useCodeMirrorEditor)
   {
      state = new ConfigurationState(state);
      state.useCodeMirrorEditor = useCodeMirrorEditor;
   }

   /**
    * Immutable object represents configuration state
    */
   public static class ConfigurationState implements IsSerializable
   {
      private boolean enterSavesApproved;
      private boolean displayButtons;
      private int pageSize;
      private NavOption navOption;
      private boolean showError;
      private boolean useCodeMirrorEditor;

      // Needed for GWT serialization
      private ConfigurationState()
      {
      }

      private ConfigurationState(ConfigurationState old)
      {
         this.enterSavesApproved = old.isEnterSavesApproved();
         this.displayButtons = old.isDisplayButtons();
         this.pageSize = old.getPageSize();
         this.navOption = old.getNavOption();
         this.showError = old.isShowError();
         this.useCodeMirrorEditor = old.isUseCodeMirrorEditor();
      }

      public boolean isEnterSavesApproved()
      {
         return enterSavesApproved;
      }

      public boolean isDisplayButtons()
      {
         return displayButtons;
      }

      public int getPageSize()
      {
         return pageSize;
      }

      public NavOption getNavOption()
      {
         return navOption;
      }

      public boolean isShowError()
      {
         return showError;
      }

      public boolean isUseCodeMirrorEditor()
      {
         return useCodeMirrorEditor;
      }
   }
}
