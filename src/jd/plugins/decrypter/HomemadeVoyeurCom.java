//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.text.DecimalFormat;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "homemade-voyeur.com" }, urls = { "http://(www\\.)?homemade\\-voyeur\\.com/(tube/video/|tube/gallery/|\\d+/)[A-Za-z0-9\\-]+\\.html" }, flags = { 0 })
public class HomemadeVoyeurCom extends PluginForDecrypt {

    public HomemadeVoyeurCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        String tempID = br.getRedirectLocation();
        // Invalid link
        if ("http://www.homemade-voyeur.com/".equals(tempID) || br.containsHTML(">404 Not Found<")) {
            logger.info("Invalid link: " + parameter);
            return decryptedLinks;
        }
        // Offline link
        if (br.containsHTML("This video does not exist\\!<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (tempID != null) {
            decryptedLinks.add(createDownloadlink(tempID));
            return decryptedLinks;
        }

        String filename = br.getRegex("<meta name=\"title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>(.+) \\- Voyeur (Videos|Pics) \\- .+</title>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<div class=\"titlerr\"[^>]+>([^\r\n]+)</div>").getMatch(0);
            }
        }

        if (parameter.contains("/tube/gallery/")) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(filename.trim());
            final DecimalFormat df = new DecimalFormat("0000");
            String[] images = br.getRegex("(https?://(www\\.)?homemade\\-voyeur\\.com/tube/images/galleries/\\d+/\\d+/[a-z0-9]{32}\\.jpg)").getColumn(0);
            for (String image : images) {
                final DownloadLink dl = createDownloadlink("directhttp://" + image);
                dl.setFinalFileName(filename + " - " + df.format(decryptedLinks.size() + 1) + image.substring(image.lastIndexOf(".")));
                fp.add(dl);
                decryptedLinks.add(dl);
            }
            return decryptedLinks;
        } else {
            tempID = br.getRegex("\"(http://api\\.slutdrive\\.com/homemadevoyeur\\.php\\?id=\\d+\\&type=v)\"").getMatch(0);
            if (tempID != null) {
                br.getPage(tempID);
                if (br.containsHTML(">404 Not Found<")) {
                    logger.info("Link offline: " + parameter);
                    return decryptedLinks;
                }
                logger.warning("Cannot handle link: " + tempID);
                logger.warning("Mainlink: " + parameter);
                return null;
            }
            tempID = br.getRegex("var playlist = \\[ \\{ url: escape\\(\\'(http://[^<>\"]*?)\\'\\) \\} \\]").getMatch(0);
            if (tempID == null) tempID = br.getRegex("(\\'|\")(http://(hosted\\.yourvoyeurvideos\\.com/videos/\\d+\\.flv|[a-z0-9]+\\.yourvoyeurvideos\\.com/mp4/\\d+\\.mp4))(\\'|\")").getMatch(1);
            if (tempID == null) tempID = br.getRegex("file=(http[^&\"]+)").getMatch(0);
            if (tempID == null || filename == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final DownloadLink dl = createDownloadlink("directhttp://" + tempID);
            dl.setFinalFileName(filename.trim() + tempID.substring(tempID.lastIndexOf(".")));
            decryptedLinks.add(dl);
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}