package org.zanata.webtrans.client.events;

import java.util.List;

import org.zanata.webtrans.client.service.GetTransUnitActionContext;
import org.zanata.webtrans.client.service.NavigationService;
import org.zanata.webtrans.shared.model.ValidationId;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.gwt.event.shared.GwtEvent;

public class FilterViewEvent extends GwtEvent<FilterViewEventHandler> implements NavigationService.UpdateContextCommand
{
   /**
    * Handler type.
    */
   private static Type<FilterViewEventHandler> TYPE;
   public static final FilterViewEvent DEFAULT = new FilterViewEvent(false, false, false, false, false, null);

   /**
    * Gets the type associated with this event.
    * 
    * @return returns the handler type
    */
   public static Type<FilterViewEventHandler> getType()
   {
      if (TYPE == null)
      {
         TYPE = new Type<FilterViewEventHandler>();
      }
      return TYPE;
   }

   private boolean filterTranslated, filterNeedReview, filterUntranslated, filterHasError;
   private boolean cancelFilter;
   private List<ValidationId> enabledValidationIds;

   public FilterViewEvent(boolean filterTranslated, boolean filterNeedReview, boolean filterUntranslated, boolean filterHasError, boolean cancelFilter, List<ValidationId> enabledValidationIds)
   {
      this.filterTranslated = filterTranslated;
      this.filterNeedReview = filterNeedReview;
      this.filterUntranslated = filterUntranslated;
      this.filterHasError = filterHasError;
      this.cancelFilter = cancelFilter;
      this.enabledValidationIds = enabledValidationIds;

   }

   @Override
   protected void dispatch(FilterViewEventHandler handler)
   {
      handler.onFilterView(this);
   }

   @Override
   public Type<FilterViewEventHandler> getAssociatedType()
   {
      return getType();
   }

   public boolean isFilterTranslated()
   {
      return filterTranslated;
   }

   public boolean isFilterNeedReview()
   {
      return filterNeedReview;
   }

   public boolean isFilterUntranslated()
   {
      return filterUntranslated;
   }

   public boolean isCancelFilter()
   {
      return cancelFilter;
   }

   public boolean isFilterHasError()
   {
      return filterHasError;
   }

   public List<ValidationId> getEnabledValidationIds()
   {
      return enabledValidationIds;
   }

   @Override
   public GetTransUnitActionContext updateContext(GetTransUnitActionContext currentContext)
   {
      Preconditions.checkNotNull(currentContext, "current context can not be null");
      return currentContext.changeFilterNeedReview(filterNeedReview).changeFilterTranslated(filterTranslated).changeFilterUntranslated(filterUntranslated).changeFilterHasError(filterHasError).changeValidationIds(enabledValidationIds);
   }

   @Override
   public String toString()
   {
      return Objects.toStringHelper(this).
            add("filterTranslated", filterTranslated).
            add("filterNeedReview", filterNeedReview).
            add("filterUntranslated", filterUntranslated).
            add("filterHasError", filterHasError).
            add("cancelFilter", cancelFilter).
            toString();
   }
}