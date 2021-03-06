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
package org.zanata.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum Constants
{
   // constants used by page and workflow objects
   propFile("setup.properties"),
   zanataInstance("zanata.instance.url"),
   projectsLink("Projects"),
   webDriverType("webdriver.type"),
   chrome, firefox, htmlUnit,
   sampleProjects("zanata.sample.projects.basedir"),
   zanataApiKey("zanata.apikey");

   public static final int FIFTY_SEC = 50000;
   private String value;

   private Constants(String value)
   {
      this.value = value;
   }

   private Constants()
   {
      this(null);
      value = name();
   }

   @Override
   public String toString()
   {
      return Objects.toStringHelper(this).add("name", name()).add("value", value).toString();
   }

   public static Properties loadProperties()
   {
      InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(propFile.value);
      Properties properties = new Properties();
      try
      {
         properties.load(inputStream);
         return properties;
      }
      catch (IOException e)
      {
         log.error("can't load {}", propFile);
         throw new IllegalStateException("can't load setup.properties");
      }
   }

   public String value()
   {
      return value;
   }
}
