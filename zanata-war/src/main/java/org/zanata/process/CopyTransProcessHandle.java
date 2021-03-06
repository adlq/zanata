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
package org.zanata.process;

import org.zanata.model.HCopyTransOptions;
import org.zanata.model.HDocument;
import org.zanata.model.HProjectIteration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * RunnableProcess Handle for a background copy trans.
 *
 * @author Carlos Munoz <a href="mailto:camunoz@redhat.com">camunoz@redhat.com</a>
 */
public class CopyTransProcessHandle extends ProcessHandle
{

   public CopyTransProcessHandle(HProjectIteration projectIteration, String triggeredBy, HCopyTransOptions options)
   {
      this.projectIteration = projectIteration;
      this.triggeredBy = triggeredBy;
      this.options = options;
   }

   public CopyTransProcessHandle(HDocument document, String triggeredBy, HCopyTransOptions options)
   {
      this.document = document;
      this.triggeredBy = triggeredBy;
      this.options = options;
   }

   @Getter
   private HProjectIteration projectIteration;

   @Getter
   private HDocument document;

   @Getter
   private String triggeredBy;

   @Getter
   private HCopyTransOptions options;

   @Getter
   @Setter
   private int documentsProcessed;

   @Getter
   @Setter
   private String cancelledBy;

   @Getter
   @Setter
   private long cancelledTime;

   // Flag to ensure max progress is only set one
   private boolean maxProgressSet = false;

   public boolean isMaxProgressSet()
   {
      return maxProgressSet;
   }

   @Override
   public void setMaxProgress(int maxProgress)
   {
      super.setMaxProgress(maxProgress);
      maxProgressSet = true;
   }
}
