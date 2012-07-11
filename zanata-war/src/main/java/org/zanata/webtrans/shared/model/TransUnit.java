package org.zanata.webtrans.shared.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.zanata.common.ContentState;
import org.zanata.common.LocaleId;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gwt.user.client.rpc.IsSerializable;

public class TransUnit implements IsSerializable
{
   private ContentState status;

   private TransUnitId id;
   private String resId;

   private LocaleId localeId;

   private boolean plural;
   private List<String> sources;
   private String sourceComment;
   private List<String> targets;
   private String msgContext;
   private String lastModifiedBy;
   private String lastModifiedTime;
   private int rowIndex;
   private int verNum;

   private static Comparator<TransUnit> rowIndexComparator;

   // for GWT
   @SuppressWarnings("unused")
   private TransUnit()
   {
   }

   //TODO make it truly an immutable class. TableEditorTableDefinition and InlineTargetCellEditor still uses some of the setter methods
   private TransUnit(TransUnitId id, String resId, LocaleId localeId, boolean plural, List<String> sources, String sourceComment, List<String> targets, ContentState status, String lastModifiedBy, String lastModifiedTime, String msgContext, int rowIndex, int verNum)
   {
      this.id = id;
      this.resId = resId;
      this.localeId = localeId;
      this.plural = plural;
      this.sources = sources;
      this.sourceComment = sourceComment;
      this.targets = targets;
      this.status = status;
      this.lastModifiedBy = lastModifiedBy;
      this.lastModifiedTime = lastModifiedTime;
      this.msgContext = msgContext;
      this.rowIndex = rowIndex;
      this.verNum = verNum;
   }
   
   public void OverrideWith(TransUnit obj){
      this.id = obj.getId();
      this.resId = obj.getResId();
      this.localeId = obj.getLocaleId();
      this.plural = obj.isPlural();
      this.sources = obj.getSources();
      this.sourceComment = obj.getSourceComment();
      this.targets = obj.getTargets();
      this.status = obj.getStatus();
      this.lastModifiedBy = obj.getLastModifiedBy();
      this.lastModifiedTime = obj.getLastModifiedTime();
      this.msgContext = obj.getMsgContext();
      this.rowIndex = obj.getRowIndex();
      this.verNum = obj.getVerNum();
   }
   
   public TransUnitId getId()
   {
      return id;
   }

   public String getResId()
   {
      return resId;
   }

   public LocaleId getLocaleId()
   {
      return localeId;
   }

   /**
    * @return the pluralSupported
    */
   public boolean isPlural()
   {
      return plural;
   }

   /**
    * @param plural the plural to set
    */
   void setPlural(boolean plural)
   {
      this.plural = plural;
   }

   public List<String> getSources()
   {
      return sources;
   }

   public void setSources(List<String> sources)
   {
      this.sources = sources;
   }

   public String getSourceComment()
   {
      return sourceComment;
   }

   public void setSourceComment(String sourceComment)
   {
      this.sourceComment = sourceComment;
   }

   public List<String> getTargets()
   {
      return targets;
   }

   public void setTargets(List<String> targets)
   {
      this.targets = targets;
   }

   public ContentState getStatus()
   {
      return status;
   }

   public void setStatus(ContentState status)
   {
      this.status = status;
   }

   public String getMsgContext()
   {
      return msgContext;
   }

   void setMsgContext(String msgContext)
   {
      this.msgContext = msgContext;
   }

   public int getRowIndex()
   {
      return rowIndex;
   }

   void setRowIndex(int rowIndex)
   {
      this.rowIndex = rowIndex;
   }

   public String getLastModifiedBy()
   {
      return lastModifiedBy;
   }

   void setLastModifiedBy(String lastModifiedBy)
   {
      this.lastModifiedBy = lastModifiedBy;
   }

   public String getLastModifiedTime()
   {
      return lastModifiedTime;
   }

   void setLastModifiedTime(String lastModifiedTime)
   {
      this.lastModifiedTime = lastModifiedTime;
   }

   public Integer getVerNum()
   {
      return verNum;
   }

   public void setVerNum(Integer verNum)
   {
      this.verNum = verNum;
   }

   public static Comparator<TransUnit> getRowIndexComparator()
   {
      if (rowIndexComparator == null)
      {
         rowIndexComparator = new Comparator<TransUnit>()
               {

            @Override
            public int compare(TransUnit o1, TransUnit o2)
            {
               if (o1 == o2)
               {
                  return 0;
               }
               if (o1 != null)
               {
                  return (o2 != null ? Integer.valueOf(o1.getRowIndex()).compareTo(o2.getRowIndex()) : 1);
               }
               return -1;
            }
         };
      }
      return rowIndexComparator;
   }

   public static class Builder
   {
      private ContentState status = ContentState.New;
      private TransUnitId id;
      private String resId;
      private LocaleId localeId;
      private boolean plural;
      private List<String> sources = Lists.newArrayList();
      private String sourceComment;
      private List<String> targets = Lists.newArrayList();
      private String msgContext;
      private String lastModifiedBy;
      private String lastModifiedTime;
      private int rowIndex;
      private int verNum = -1; // to fail check if not set before build

      private Builder(TransUnit transUnit)
      {
         this.status = transUnit.status;
         this.id = transUnit.id;
         this.resId = transUnit.resId;
         this.localeId = transUnit.localeId;
         this.plural = transUnit.plural;
         this.sources = nullToEmpty(transUnit.sources);
         this.sourceComment = transUnit.sourceComment;
         this.targets = nullToEmpty(transUnit.targets);
         this.msgContext = transUnit.msgContext;
         this.lastModifiedBy = transUnit.lastModifiedBy;
         this.lastModifiedTime = transUnit.lastModifiedTime;
         this.rowIndex = transUnit.rowIndex;
         this.verNum = transUnit.verNum;
      }

      private Builder()
      {
      }

      public TransUnit build()
      {
         Preconditions.checkNotNull(id, "transUnitId can not be null");
         Preconditions.checkNotNull(resId, "resId can not be null");
         Preconditions.checkNotNull(localeId, "localeId can not be null");
         Preconditions.checkState(sources != null && !sources.isEmpty());
         Preconditions.checkState(rowIndex >= 0);
         Preconditions.checkState(verNum >= 0);

         lastModifiedBy = Strings.nullToEmpty(lastModifiedBy);
         lastModifiedTime = Strings.nullToEmpty(lastModifiedTime);
         status = Objects.firstNonNull(status, ContentState.New);

         return new TransUnit(id, resId, localeId, plural, sources, sourceComment, targets, status, lastModifiedBy, lastModifiedTime, msgContext, rowIndex, verNum);
      }

      public static Builder newTransUnitBuilder()
      {
         return new Builder();
      }

      public static Builder from(TransUnit transUnit)
      {
         return new Builder(transUnit);
      }

      private static List<String> nullToEmpty(List<String> contents)
      {
         return contents == null ? Lists.<String>newArrayList() : contents;
      }

      public Builder setStatus(ContentState status)
      {
         this.status = status;
         return this;
      }

      public Builder setId(TransUnitId id)
      {
         this.id = id;
         return this;
      }

      public Builder setId(long id)
      {
         this.id = new TransUnitId(id);
         return this;
      }

      public Builder setResId(String resId)
      {
         this.resId = resId;
         return this;
      }

      public Builder setLocaleId(LocaleId localeId)
      {
         this.localeId = localeId;
         return this;
      }

      public Builder setLocaleId(String localeString)
      {
         this.localeId = new LocaleId(localeString);
         return this;
      }

      public Builder setPlural(boolean plural)
      {
         this.plural = plural;
         return this;
      }

      public Builder setSources(List<String> sources)
      {
         this.sources = nullToEmpty(sources);
         return this;
      }

      public Builder addSource(String... sourceStrings)
      {
         Collections.addAll(sources, sourceStrings);
         return this;
      }

      public Builder setSourceComment(String sourceComment)
      {
         this.sourceComment = sourceComment;
         return this;
      }

      public Builder setTargets(List<String> targets)
      {
         this.targets = nullToEmpty(targets);
         return this;
      }

      public Builder addTargets(String... targetStrings)
      {
         Collections.addAll(targets, targetStrings);
         return this;
      }

      public Builder setMsgContext(String msgContext)
      {
         this.msgContext = msgContext;
         return this;
      }

      public Builder setLastModifiedBy(String lastModifiedBy)
      {
         this.lastModifiedBy = lastModifiedBy;
         return this;
      }

      public Builder setLastModifiedTime(String lastModifiedTime)
      {
         this.lastModifiedTime = lastModifiedTime;
         return this;
      }

      public Builder setRowIndex(int rowIndex)
      {
         this.rowIndex = rowIndex;
         return this;
      }

      public Builder setVerNum(int verNum)
      {
         this.verNum = verNum;
         return this;
      }
   }
}
