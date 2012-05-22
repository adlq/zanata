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
package org.zanata.page;

import java.util.Collection;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

public class ProjectPage extends AbstractPage
{
   private static final Logger LOGGER = LoggerFactory.getLogger(ProjectPage.class);

   @FindBy(id = "main_content")
   private WebElement mainContent;
   private final List<WebElement> h1;

   @FindBy(linkText = "Create Version")
   private WebElement createVersionLink;

   @FindBy(id = "main_content:activeIterations")
   private WebElement activeVersions;

   public ProjectPage(final WebDriver driver)
   {
      super(driver);
      h1 = mainContent.findElements(By.tagName("h1"));
      Preconditions.checkState(h1.size() >= 2, "should have at least 2 <h1> under main content");
   }

   public String getProjectId()
   {
      return h1.get(0).getText();
   }

   public String getProjectName()
   {
      return h1.get(1).getText();
   }

   public CreateVersionPage clickCreateVersionLink()
   {
      createVersionLink.click();
      return new CreateVersionPage(getDriver());
   }

   public ProjectVersionPage goToActiveVersion(final String versionId)
   {
      List<WebElement> versionLinks = activeVersions.findElements(By.tagName("a"));
      LOGGER.info("found {} active versions", versionLinks.size());

      Preconditions.checkState(!versionLinks.isEmpty(), "no version links available");
      Collection<WebElement> found = Collections2.filter(versionLinks, new Predicate<WebElement>()
      {
         @Override
         public boolean apply(WebElement input)
         {
            // the link text has line break in it
            String linkText = input.getText().replaceAll("\\n", " ");
            return linkText.matches(versionId + "\\s+Documents:.+");
         }
      });
      Preconditions.checkState(found.size() == 1, versionId + " not found");
      found.iterator().next().click();
      return new ProjectVersionPage(getDriver());
   }
}