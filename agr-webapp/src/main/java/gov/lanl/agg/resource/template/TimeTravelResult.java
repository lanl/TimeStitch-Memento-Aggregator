package gov.lanl.agg.resource.template;

/**
 * @author: Harihar Shankar, 6/11/14 1:03 PM
 */
public class TimeTravelResult {

    public String getTemplate() {

        StringBuilder template = new StringBuilder();

        template.append("<!DOCTYPE html>\n" +
                        "<html>" +
                "<head>\n" +
                        "<meta charset=\"utf-8\" />\n" +
                "  <title>##page_title##</title>\n" +
                "  <link rel=\"stylesheet\" href=\"http://mementoweb.org/static/css/jquery-ui-1.10.4.min.css\">\n" +
                "  <link rel=\"stylesheet\" href=\"http://mementoweb.org/static/css/timetravel.css\">\n" +
                "<link rel=\"icon\" type=\"image/png\" href=\"http://mementoweb.org/static/css/images/timetravel_logo_20x20.png\">" +
                // "  <link rel=\"stylesheet\" href=\"/css/font-awesome.min.css\">\n" +
                //"<link href=\"//maxcdn.bootstrapcdn.com/font-awesome/4.1.0/css/font-awesome.min.css\" rel=\"stylesheet\">" +
                "  <script src=\"http://mementoweb.org/static/js/jquery-1.10.2.js\"></script>\n" +
                "  <script src=\"http://mementoweb.org/static/js/jquery-ui-1.10.4.min.js\"></script>\n" +
                //"  <script src=\"http://mementoweb.org/static/js/d3.min.js\"></script>\n" +
                "  <script src=\"http://mementoweb.org/static/js/timetravel.js\"></script>\n" +
                "  <script src=\"http://mementoweb.org/static/js/mementoutils.js\"></script>\n" +
                "  <script src=\"http://mementoweb.org/static/js/linkdecor.js\"></script>\n" +
                "  <script>        " +
                        "##script##</script>\n" +
                //        "<script type=\"text/javascript\">var switchTo5x=true;</script>\n" +
                        //"<script type=\"text/javascript\" src=\"http://w.sharethis.com/button/buttons.js\"></script>\n" +
                //        "<script type=\"text/javascript\">stLight.options({publisher: \"cf773161-c2cd-4e53-97ef-af4d0e653322\", doNotHash: false, doNotCopy: false, hashAddressBar: false});</script>" +
                "</head>"
        );

        template.append("<body>\n" +

                "   <div id=\"menu_top\">\n" +
                        "        <iframe id=\"menu_top_frame\" src=\"http://mementoweb.org/static/menu_top_tt.html\" style=\"border:0;width:980px;height:102px;\"></iframe>\n" +
                        "    </div>" +
                "    <div id='header'>\n" +
                //"        <a href='/'><img id='memento_logo' src='/css/images/timetravel_logo.png' alt='Home' /></a>\n" +
                "        <input type=\"text\" id=\"url\" class='ui-corner-all result_input' value='http://' title='Enter URL for Time Travel.' />\n" +
                "        <br/>\n" +
                "        <input type=\"text\" id=\"datepicker\" class='result_input ui-corner-all' title='YYYY-MM-DD' />\n" +
                "        <input type=\"text\" id=\"timepicker\" class='result_input ui-corner-all' title='HH:mm:ss' />\n" +
                "        <button id='search' name='search' class='result_buttons'>Find</button>\n" +
                     //   "<button id='reconstruct' name='reconstruct' class='result_buttons'>Reconstruct</button>\n" +
                        "##memento_summary##" +
                "    </div>\n" +
                        "<div id='right_pane'>" +
                          //"<div id='social_share'>" +
                          //  "<span class='st_facebook_large' displayText='Facebook'></span>\n" +
                          //  "<span class='st_twitter_large' displayText='Tweet'></span>\n" +
                          //  "<span class='st_googleplus_large' displayText='Google +'></span>\n" +
                          //  "<span class='st_pinterest_large' displayText='Pinterest'></span>\n" +
                          //  "<span class='link_decoration_sp' title='Robust Links'>" +
                          //  "<img src='http://mementoweb.org/static/css/images/robustlinks.png' id='link_decoration' style='width: 32px; height: 32px;' />" +
                          //  "</span>" +
                          //"</div>" +
                        "<div id='link_decoration_dialog'></div>" +
                          "<div id='memento_chrome'>" +
                            "<a href='http://bit.ly/memento-for-chrome' target='_blank'>" +
                            "<img class='right_pane_img' src='http://mementoweb.org/static/css/images/mem_for_chrome.png' alt='Memento for Chrome'/>" +
                            "</a>" +
                          "</div>" +
                        "<div id='memento_mediawiki'>" +
                        "<a href='http://bit.ly/memento-for-mediawiki' target='_blank'>" +
                        "<img class='right_pane_img' src='http://mementoweb.org/static/css/images/memento_for_mediawiki.png' alt='Memento for MediaWiki'/>" +
                        "</a>" +
                        "</div>" +
                        "<div id='use_robust_links'>" +
                        "<a href='http://robustlinks.mementoweb.org' target='_blank'>" +
                        "<img class='right_pane_img' src='http://mementoweb.org/static/css/images/use_robust_links.png' alt='Use Robust Links'/>" +
                        "</a>" +
                        "</div>" +
                        "<div id='zotero_robust_links'>" +
                        "<a href='http://robustlinks.mementoweb.org/zotero/' target='_blank'>" +
                        "<img class='right_pane_img' src='http://mementoweb.org/static/css/images/zotero_robust_links.png' alt='Zotero Robust Links'/>" +
                        "</a>" +
                        "</div>" +
                        "</div>" +
                        "<div id='result_wrapper'>" +
                        "<div class='memento_messages'>##memento_messages##</div>" +
                "    ##archive_result##" +
                        "</div>"
        );
        template.append("</body>\n" +
                "</html>");

        return template.toString();
    }

    public String getMementoSummary() {
        return "       <span id='memento_info'>" +
                      "          <a id='prev_memento' href='##prev_timetravel_memento_url##' title='Previous: ##prev_timetravel_memento_dt##' class='memento_buttons ui-state-default ui-corner-all'>" +
                      //  "          <a id='prev_memento' href='##prev_timetravel_memento_url##' title='Previous: ##prev_timetravel_memento_dt##' class='memento_buttons ui-state-default'>" +
                      //  "<span class='fa fa-caret-square-o-left'></span>" +
                        "</a>" +
                        "          <span id='this_memento' title='Current: ##curr_timetravel_memento_dt##' class='this_memento ui-state-default ui-corner-all'>" +
                        "          ##curr_timetravel_memento_dt##</span>" +
                        "          <a id='next_memento' href='##next_timetravel_memento_url##' title='Next: ##next_timetravel_memento_dt##' class='memento_buttons ui-state-default ui-corner-all'>" +
                      //  "<span class='fa fa-caret-square-o-right'></span>" +
                        "          </a>" +
                        "    </span>\n";
    }

    public String getArchiveResultTemplate() {

        String archiveResult = "";

        archiveResult = "<div class='results'>\n" +
                "        <div class='memento-links'>\n" +
                "            <span class='result_heading'>\n" +
                "                <a href='##memento_redirect_url##'>\n" +
                //"                    ##memento_delta_title##</a>, from ##archive_title##</span>" +
                "                      ##archive_title## Memento, ##memento_delta_title##</a></span>" +
                //"            ##memento_adjective## Memento</span>\n" +
                "            <p class='result_archive_url'>##memento_url##</p>" +
                "            <p class='result_archive_dt'>##memento_dt##" +
                "&nbsp; [ ##memento_delta## from requested date ]##native_archive_message##</p>\n" +
                "            <div class='sub-results'>\n" +
                //"                <div class='more_mementos'>" +
                //"                <span class='more_mementos_icon' style='display: inline-block; float: left;'></span>" +
                //"                <span class='more_mementos_title'>Special Mementos</span>" +
                //"                 </div>" +
                "                <table class='sub-results-row'>" +
                "                  <tr>" +
                "                    <td class='left_label'>##prev_memento##</td>\n" +
                "                    <td class='right_label'>##next_memento##</td>\n" +
                "                </tr><tr>\n" +
                "                    <td class='left_label'>##first_memento##</td>\n" +
                "                    <td class='right_label'>##last_memento##</td>\n" +
                "                  </tr>" +
                "                </table>" +
                "            </div>\n" +
                "            ##archive_calendar_page##" +
                "        </div>\n" +
                "    </div>\n";

        return archiveResult;
    }

    /*
    public String getArchiveResultTemplate() {

        String archiveResult = "";

        archiveResult = "<div class='results'>\n" +
                "        <div class='memento-links'>\n" +
                "            <span class='result_heading'>\n" +
                "                <a href='##memento_redirect_url##'>\n" +
                "                    ##archive_title##</a></span>" +
                //"            ##memento_adjective## Memento</span>\n" +
                "            <p class='result_archive_url'>##memento_url##</p>" +
                "            <p class='result_archive_dt'>##memento_dt##" +
                "<br/>[ ##memento_delta## from requested date ]##native_archive_message##</p>\n" +
                "            <div class='sub-results'>\n" +
                "                <table class='sub-results-row'>" +
                "                  <tr>" +
                "                    <td class='left_label'>##prev_memento##</td>\n" +
                "                    <td class='right_label'>##next_memento##</td>\n" +
                "                </tr><tr>\n" +
                "                    <td class='left_label'>##first_memento##</td>\n" +
                "                    <td class='right_label'>##last_memento##</td>\n" +
                "                  </tr>" +
                "                </table>" +
                "            </div>\n" +
                "            ##archive_calendar_page##" +
                "        </div>\n" +
                "    </div>\n";

        return archiveResult;
    }
    */

    public String getArchiveCalendarPageTemplate() {
        String archiveCalendarPage;

        archiveCalendarPage = "<div class='archive-calendar-page'>\n" +
                //"                <img class='archive-img' src='##archive_img_url##'/>\n" +
                "                <a href='##archive_calendar_url##'>All captures from ##archive_name## between ##first_memento_year## and ##last_memento_year##</a>\n" +
                "            </div>\n";

        return archiveCalendarPage;
    }

    public String getNativeArchiveMessageTemplate() {
        String archiveCalendarPage;

        archiveCalendarPage = "<br/>[ This version was active between ##native_memento_from## and ##native_memento_to##. ]";

        return archiveCalendarPage;
    }

    public String getSubResultTemplate() {

        return "<a href='##sub_result_memento_redirect_url##'>\n" +
                "##sub_result_type_title##</a>" +
                "<br/>" +
                "<p class='sub_sub_result'>" +
                "##sub_result_memento_dt##<br/>[ ##sub_result_memento_delta## ]" +
                "</p>";

    }

    public String getUnauthorizedErrorTemplate() {
        return "<div class='memento_error'>" +
                "<span style='float: left'>" +
                "<img src='http://mementoweb.org/static/css/images/rjo.jpg' style='width: 400px; height: 400px;' />" +
                "<p style='font-size: 0.7em; color: #ccc'>" +
                "Photo by RENE BURRI / MAGNUM PHOTOS" +
                "<br/>Blacklist filter obtained from <a href='http://dsi.ut-capitole.fr/blacklists/index_en.php'" +
                " target='_blank' >The Université Toulouse 1 Capitole</a>." +
                "</p>" +
                "</span>" +
                "<span style='float: right; position: absolute; width: 350px; padding-left: 10px;'>" +
                "<p>This service is provided by the Los Alamos National Laboratory." +
                " Unfortunately, the URL you were searching for is blocked by our service,"
                + " which means that no Mementos are returned in the result list." +
                " Please note, however, that it is entirely possible that Mementos exist in one or more publicly available web archives.</p>" +
                "</span>" +
                "</div>";
    }

    public String getFutureTimeErrorTemplate() {
        return "<div class='memento_error'>" +
                "<span style='float: left'>" +
                "<img src='http://mementoweb.org/static/css/images/tardis.jpg' style='width: 300px; height: 400px;' />" +
                "<p style='font-size: 0.7em; color: #ccc'>" +
                "Photo by <a href='http://www.flickr.com/photos/nez/' target='_blank'>" +
                "Andrew</a>" +
                "</p>" +
                "</span>" +
                "<span style='float: right; position: absolute; width: 450px; padding-left: 10px;'>" +
                "<p>Despite this service being provided by Los Alamos National Laboratory, " +
                "time travel into the future is not yet supported. " +
                "We are working hard to address this deficiency.</p>" +
                "</span>" +
                "</div>";
    }

    public String getErrorTemplate() {
        return "<div class='memento_error'>" +
                "<p>No mementos were found for the requested URI and datetime.</p>" +
                "<p><h3>Possible reasons:</h3>" +
                "<ul><li>Check if the URI entered is a valid HTTP URI.</li>" +
                "<li>The date and time is expected in the format YYYY-MM-DD hh:mm:ss.</li>" +
                "<li>It is possible that there are no mementos for the URI." +
                "</ul></p>"+
                "</div>";
    }

    public String getRequestURLChangesTemplate() {
        /*
        return "<div class='memento_messages'>" +
                "<p><b>Showing results for:</b> ##original_url##</p>" +
                "</div>";
        */
        return "Showing results for: ##original_url##";
    }

    public String getPageTitleTemplate() {
        return "Mementos for ##title_original_url## around ##title_req_datetime##: ##title_timetravel_url## #memento - ";
    }

    public String getMapTemplate() {

        StringBuilder template = new StringBuilder();

        template.append("<!DOCTYPE html>\n" +
                        "<head>\n" +
                        "<meta charset=\"utf-8\" />\n" +
                        "  <title>##page_title##</title>\n" +
                        "  <link rel=\"stylesheet\" href=\"/css/jquery-ui-1.10.4.min.css\">\n" +
                        "  <link rel=\"stylesheet\" href=\"/css/timetravel.css\">\n" +
                        "  <script src=\"/static/js/jquery-1.10.2.js\"></script>\n" +
                        "  <script src=\"/static/js/jquery-ui-1.10.4.min.js\"></script>\n" +
                        "  <script src=\"/static/js/d3.min.js\"></script>\n" +
                        "  <script src=\"/static/js/timetravel_map.js\"></script>\n" +
                        "  <script src=\"/static/js/mementoutils.js\"></script>\n" +
                        "  <script>##script##</script>\n" +
                        "<script type=\"text/javascript\">var switchTo5x=true;</script>\n" +
                        "<script type=\"text/javascript\" src=\"http://w.sharethis.com/button/buttons.js\"></script>\n" +
                        "<script type=\"text/javascript\">stLight.options({publisher: \"cf773161-c2cd-4e53-97ef-af4d0e653322\", doNotHash: false, doNotCopy: false, hashAddressBar: false});</script>" +
                        "</head>"
        );

        template.append("<body style='width: 1080px;'>\n" +
                        "    <div id=\"list_menu_top\"></div>\n" +
                        "    <div id='header' style='height: 88px;'>\n" +
                        "        <a href='/'><img id='memento_logo' src='/static/css/images/memento.png' alt='Home' /></a>\n" +
                        "<h3>Time Map distribution for ##original_url##</h3>" +
                        "    </div>\n" +
                        "<div id='right_pane'>" +
                        "<div id='social_share'>" +
                        "<span class='st_facebook_large' displayText='Facebook'></span>\n" +
                        "<span class='st_twitter_large' displayText='Tweet'></span>\n" +
                        "<span class='st_googleplus_large' displayText='Google +'></span>\n" +
                        "<span class='st_pinterest_large' displayText='Pinterest'></span>\n" +
                        "<span class='st_email_large' displayText='Email'></span>" +
                        "</div>" +
                        "<div id='memento_chrome'>" +
                        "<a href='http://bit.ly/memento-for-chrome' target='_blank'>" +
                        "<img class='right_pane_img' src='/static/css/images/mem_for_chrome.png' />" +
                        "</a>" +
                        "</div>" +
                        "<div id='memento_mediawiki'>" +
                        "<a href='http://bit.ly/memento-for-mediawiki' target='_blank'>" +
                        "<img class='right_pane_img' src='/static/css/images/memento_for_mediawiki.png' />" +
                        "</a>" +
                        "</div>" +
                        "</div>" +
                        "<div id='result_wrapper'>" +
                        "<div class='chart_infobox'></div>" +
                        "    ##archive_result##" +
                        "</div>" +
                        "<div id=\"list_footer\"></div>\n" +
                        "  <script>" +
                        "  (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){\n" +
                        "  (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),\n" +
                        "  m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)\n" +
                        "  })(window,document,'script','//www.google-analytics.com/analytics.js','ga');\n" +
                        "\n" +
                        "  ga('create', 'UA-10627462-1', 'mementoweb.org');\n" +
                        "  ga('send', 'pageview');\n" +
                        "\n" +
                        "  </script>"
        );
        template.append("</body>\n" +
                "</html>");

        return template.toString();
    }

    public String getMapResultTemplate() {

        return "<div id='##archive_id##_result'>\n" +
                "<h3 id='##archive_id##_title' class='map_archive_title'>" +
                "##archive_full_name##</h3>" +
                "<div id='##archive_id##_timeline' class='timeline'></div>" +
                "</div>";
    }
}
