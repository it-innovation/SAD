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
package uk.ac.soton.itinnovation.sad.service.helpers;

import com.sun.syndication.feed.rss.Channel;
import com.sun.syndication.feed.rss.Content;
import com.sun.syndication.feed.rss.Item;
import uk.ac.soton.itinnovation.sad.service.domain.RssItem;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.view.feed.AbstractRssFeedView;

/**
 * Builds SAD RSS feeds.
 */
public class RssViewer extends AbstractRssFeedView {

    @Override
    protected void buildFeedMetadata(Map<String, Object> model, Channel feed, HttpServletRequest request) {

        String feedTitle = (String) model.get("feedTitle");
        String feedDescription = (String) model.get("feedDescription");
        String feedLink = (String) model.get("feedLink");
        Date buildDate = (Date) model.get("buildDate");

        feed.setTitle(feedTitle);
        feed.setDescription(feedDescription);
        feed.setLink(feedLink);
        feed.setLastBuildDate(buildDate);

//        feed.setCopyright("Experimedia consortium 2013");

        super.buildFeedMetadata(model, feed, request);
    }

    @Override
    protected List<Item> buildFeedItems(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

        @SuppressWarnings("unchecked")
        List<RssItem> listContent = (List<RssItem>) model.get("feedContent");
        List<Item> items = new ArrayList<>(listContent.size());

        Item item; Content content;
        for (RssItem tempContent : listContent) {

            item = new Item(); content = new Content();

            if (tempContent.getSummary() != null) {
                content.setValue(tempContent.getSummary());
                item.setContent(content);
            }

            item.setTitle(tempContent.getTitle());
            item.setLink(tempContent.getUrl());
            item.setPubDate(tempContent.getCreatedDate());

            if (tempContent.getAuthor() != null) {
                item.setAuthor(tempContent.getAuthor());
            }

            items.add(item);
        }

        return items;
    }
}
