package org.zanata.page.groups;

import java.util.List;

import javax.annotation.Nullable;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.FluentWait;
import org.zanata.page.AbstractPage;
import org.zanata.util.TableRow;
import org.zanata.util.WebElementUtil;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Slf4j
public class VersionGroupPage extends AbstractPage
{
   public static final int PROJECT_NAME_COLUMN = 0;
   @FindBy(linkText = "Add Project Versions")
   private WebElement addProjectVersionsLink;

   @FindBy(id = "versionAddPanelContentDiv")
   private WebElement addProjectVersionPanel;

   @FindBy(xpath = "//table[contains(@id, 'iterationsDataTable')]")
   private WebElement groupDataTable;

   public VersionGroupPage(final WebDriver driver)
   {
      super(driver);
   }

   public VersionGroupPage addProjectVersion()
   {
      addProjectVersionsLink.click();
      waitForTenSec().until(new Function<WebDriver, WebElement>()
      {
         @Override
         public WebElement apply(WebDriver driver)
         {
            return addProjectVersionPanel.findElement(By.id("projectVersionSearch:searchForm:searchField"));
         }
      });
      return new VersionGroupPage(getDriver());
   }

   public List<List<String>> searchProject(final String projectName, final int expectedResultNum)
   {
      WebElement searchField = addProjectVersionPanel.findElement(By.id("projectVersionSearch:searchForm:searchField"));
      searchField.sendKeys(projectName);

      WebElement searchButton = addProjectVersionPanel.findElement(By.id("projectVersionSearch:searchForm:searchBtn"));
      searchButton.click();

      return waitForTenSec().until(new Function<WebDriver, List<List<String>>>()
      {
         @Override
         public List<List<String>> apply(WebDriver driver)
         {
            WebElement table = addProjectVersionPanel.findElement(By.id("projectVersionSearch:searchResults:resultTable"));
            List<TableRow> tableRows = WebElementUtil.getTableRows(table);
            // we want to wait until search result comes back. There is no way we can tell whether search result has come back and table refreshed.
            // To avoid the org.openqa.selenium.StaleElementReferenceException (http://seleniumhq.org/exceptions/stale_element_reference.html),
            // we have to set expected result num

            List<List<String>> tableContents = WebElementUtil.transformToTwoDimensionList(tableRows);
            Iterable<List<String>> filter = Iterables.filter(tableContents, new Predicate<List<String>>()
            {
               @Override
               public boolean apply(List<String> input)
               {
                  return input.get(0).contains(projectName);
               }
            });
            if (Iterables.size(filter) != expectedResultNum)
            {
               log.debug("waiting for search result refresh...");
               return null;
            }
            return tableContents;
         }
      });
   }

   public VersionGroupPage addToGroup(int rowIndex)
   {
      WebElement table = addProjectVersionPanel.findElement(By.id("projectVersionSearch:searchResults:resultTable"));

      List<WebElement> cells = WebElementUtil.getTableRows(table).get(rowIndex).getCells();
      WebElement actionCell = cells.get(cells.size() - 1);
      if (!actionCell.getText().contains("Already in Group"))
      {
         WebElement selectCheckBox = actionCell.findElement(By.xpath(".//input[@type='checkbox']"));
         if (!selectCheckBox.isSelected())
         {
            selectCheckBox.click();
         }
      }

      WebElement addSelected = addProjectVersionPanel.findElement(By.id("projectVersionSearch:searchResults:addSelectedBtn"));
      addSelected.click();
      return this;
   }

   public VersionGroupPage closeSearchResult()
   {
      WebElement closeButton = addProjectVersionPanel.findElement(By.id("projectVersionSearch:searchForm:closeBtn"));
      closeButton.click();
      return this;
   }

   @SuppressWarnings("unused")
   public List<List<String>> getProjectVersionsInGroup()
   {
      List<TableRow> tableRows = WebElementUtil.getTableRows(groupDataTable);
      return WebElementUtil.transformToTwoDimensionList(tableRows);
   }

   @SuppressWarnings("unused")
   public List<List<String>> getNotEmptyProjectVersionsInGroup()
   {
      FluentWait<WebDriver> fluentWait = waitForTenSec();
      fluentWait.ignoring(IllegalStateException.class);
      List<TableRow> tableRows = fluentWait.until(new Function<WebDriver, List<TableRow>>()
      {
         @Override
         public List<TableRow> apply(WebDriver driver)
         {
            final List<TableRow> tableRows = WebElementUtil.getTableRows(groupDataTable);

            log.info("{} project versions for this group", tableRows.size());
            Preconditions.checkState(tableRows.size() > 0, "table is empty");
            return tableRows;
         }
      });

      return WebElementUtil.transformToTwoDimensionList(tableRows);
   }

}
