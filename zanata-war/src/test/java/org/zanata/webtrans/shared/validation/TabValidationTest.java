/*
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.zanata.webtrans.shared.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.zanata.webtrans.client.resources.ValidationMessages;
import org.zanata.webtrans.server.locale.Gwti18nReader;
import org.zanata.webtrans.shared.model.ValidationId;
import org.zanata.webtrans.shared.validation.action.TabValidation;

@Test(groups = { "unit-tests" })
public class TabValidationTest
{
   private ValidationMessages messages;
   private TabValidation validation;
   private static final List<String> noErrors = Collections.<String>emptyList();

   @BeforeMethod
   public void init() throws IOException
   {
      messages = Gwti18nReader.create(ValidationMessages.class);
      validation = new TabValidation(ValidationId.TAB, messages);
      validation.getValidationInfo().setEnabled(true);
   }

   @Test
   public void idIsSet()
   {
      assertThat(validation.getId(), is(ValidationId.TAB));
   }

   @Test
   public void noTabsInEither()
   {
      String source = "Source without tab";
      String target = "Target without tab";
      List<String> errorList = validation.validate(source, target);

      assertThat(errorList, is(noErrors));
   }

   @Test
   public void tabsInBoth()
   {
      String source = "Source with\ttab";
      String target = "Target with\ttab";
      List<String> errorList = validation.validate(source, target);

      assertThat(errorList, is(noErrors));
   }

   @Test
   public void noTabsInTarget()
   {
      String source = "Source with\ttab";
      String target = "Target without tab";
      List<String> errorList = validation.validate(source, target);

      assertThat(errorList, hasItem(messages.targetHasFewerTabs(1, 0)));
      assertThat(errorList.size(), is(1));
      
   }

   @Test
   public void noTabsInSource()
   {
      String source = "Source without tab";
      String target = "Target with\textra tab";
      List<String> errorList = validation.validate(source, target);

      assertThat(errorList, hasItem(messages.targetHasMoreTabs(0, 1)));
      assertThat(errorList.size(), is(1));
      
   }

   @Test
   public void fewerTabsInTarget()
   {
      String source = "Source with two\t\t tabs";
      String target = "Target with one\ttab";
      List<String> errorList = validation.validate(source, target);

      assertThat(errorList, hasItem(messages.targetHasFewerTabs(2, 1)));
      assertThat(errorList.size(), is(1));
      
   }

   @Test
   public void moreTabsInTarget()
   {
      String source = "Source with one\ttab";
      String target = "Target with two\t\t tabs";
      List<String> errorList = validation.validate(source, target);

      assertThat(errorList, hasItem(messages.targetHasMoreTabs(1, 2)));
      assertThat(errorList.size(), is(1));
      
   }

}
