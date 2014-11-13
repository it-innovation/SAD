/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2013
//
// Copyright in this library belongs to the University of Southampton
// IT Innovation Centre of Gamma House, Enterprise Road,
// Chilworth Science Park, Southampton, SO16 7NS, UK.
//
// This software may not be used, sold, licensed, transferred, copied
// or reproduced in whole or in part in any manner or form or in or
// on any media by any person other than in accordance with the terms
// of the Licence Agreement supplied with the software, or otherwise
// without the prior written consent of the copyright owners.
//
// This software is distributed WITHOUT ANY WARRANTY, without even the
// implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
// PURPOSE, except where stated in the Licence Agreement supplied with
// the software.
//
//	Created By :			Maxim Bashevoy
//	Created Date :			2013-01-30
//	Created for Project :           Experimedia
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.sad.service.controllers;

import uk.ac.soton.itinnovation.sad.service.domain.RssItem;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 * Generates RSS feeds for SAD Service.
 */
@Controller
@RequestMapping("/rss")
public class RssController {

    private final org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Returns RSS for the whole SAD service.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/feed.xml")
    public ModelAndView getFeedInRss() {

        logger.debug("Returning sample RSS feed");

        List<RssItem> items = new ArrayList<RssItem>();

        RssItem content = new RssItem();
        content.setTitle("Facebook search");
        content.setUrl("http://localhost:8081/SAD/sadService/sample/getlastrun");
        content.setSummary("Output of facebook search plugin");
        content.setCreatedDate(new Date());
        items.add(content);

        RssItem content2 = new RssItem();
        content2.setTitle("Twitter search and analysis");
        content2.setUrl("http://localhost:8081/SAD/sadService/sample/getlastrun");
        content2.setSummary("Output of twitter search and analysis plugin");
        content2.setCreatedDate(new Date());
        items.add(content2);

        ModelAndView mav = new ModelAndView();
        mav.setViewName("rssViewer");
        mav.addObject("feedContent", items);
        mav.addObject("feedTitle", "Sample RSS feed");
        mav.addObject("feedDescription", "Sample RSS feed description");
        mav.addObject("feedLink", "#");
        mav.addObject("buildDate", new Date());

        return mav;

    }
}
