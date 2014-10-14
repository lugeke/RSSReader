package com.example.lugeke.rssreader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.text.Html;

import com.example.lugeke.rssreader.provider.FeedContract;

/**
 *
 *继承 defaultHandler 处理xml文件
 *
 */
public class RSSHandler extends DefaultHandler {

    // rss xml文件的各个标签的名称
    private static final String ANDRHOMBUS = "&#";
    private static final String TAG_RSS = "rss";
    private static final String TAG_RDF = "rdf";
    private static final String TAG_FEED = "add_feed";
    private static final String TAG_ENTRY = "entry";
    private static final String TAG_ITEM = "item";
    private static final String TAG_UPDATED = "updated";
    private static final String TAG_TITLE = "title";
    private static final String TAG_LINK = "link";
    private static final String TAG_DESCRIPTION = "description";
    private static final String TAG_MEDIA_DESCRIPTION = "media:description";
    private static final String TAG_CONTENT = "content";
    private static final String TAG_MEDIA_CONTENT = "media:content";
    private static final String TAG_ENCODEDCONTENT = "encoded";
    private static final String TAG_SUMMARY = "summary";
    private static final String TAG_PUBDATE = "pubDate";
    private static final String TAG_DATE = "date";
    private static final String TAG_LASTBUILDDATE = "lastBuildDate";
    private static final String TAG_AUTHOR = "author";
    private static final String TAG_NAME = "name";
    private static final String TAG_CREATOR = "creator";
    private static final String ATTRIBUTE_HREF = "href";
    private static final String SPACE = " ";
    private static final String TWOSPACE = "  ";
    private static final String HTML_SPAN_REGEX = "<[/]?[ ]?span(.|\n)*?>";
    public static final String COMMASPACE = ", ";

    private static final String[] TIMEZONES = { "MEST", "EST", "PST" };// 时区
    private static final String[] TIMEZONES_REPLACE = { "+0200", "-0500",
            "-0800" };
    private static final int TIMEZONES_COUNT = 3;
    private static final DateFormat[] PUBDATE_DATEFORMATS = {// 日期格式
            new SimpleDateFormat("EEE', 'd' 'MMM' 'yyyy' 'HH:mm:ss' 'Z",
                    Locale.US),
            new SimpleDateFormat("d' 'MMM' 'yyyy' 'HH:mm:ss' 'Z", Locale.US),
            new SimpleDateFormat("EEE', 'd' 'MMM' 'yyyy' 'HH:mm:ss' 'z",
                    Locale.US),

    };
    private static final int PUBDATEFORMAT_COUNT = 3;
    private static final DateFormat[] UPDATE_DATEFORMATS = {
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSz", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US), };
    private static final int DATEFORMAT_COUNT = 3;
    private static final String Z = "Z";
    private static final String GMT = "GMT";// 格林威治标准时间
    public static final String DB_ISNULL = " IS NULL ";

    private Context context;
    private Date insertUpdate;
    long id;

    // 判断是否进入过标签
    private boolean titleTagEntered;
    private boolean updatedTagEntered;
    private boolean linkTagEntered;
    private boolean descriptionTagEntered;
    private boolean pubDateTagEntered;
    private boolean dateTagEntered;
    private boolean lastUpdateDateTagEntered;
    private boolean feedRefreshed;
    private boolean nameTagEntered;
    private boolean authorTagEntered;

    // 需要提取的信息
    private StringBuilder title;
    private StringBuilder dateStringBuilder;
    private Date entryDate;
    private StringBuilder entryLink;
    private StringBuilder description;
    private Date keepDateBorder;
    private long realLastUpdate;
    private StringBuilder author;

    // html编码的格式转换
    public static final String HTML_LT = "&lt;";
    public static final String HTML_GT = "&gt;";
    public static final String LT = "<";
    public static final String HTML_QUOT = "&quot;";
    public static final String QUOT = "\"";
    public static final String HTML_APOS = "&apos;";
    public static final String APOSTROPHE = "'";
    public static final String AMP = "&";
    public static final String AMP_SG = "&amp;";
    public static final String GT = ">";
    public static final String EMPTY = "";
    public static final String HTML_TAG_REGEX = "<(.|\n)*?>";
    private static final long KEEP_TIME = 30 * 86400000l;

    public RSSHandler(Context context) {
        this.context = context;
    }

    /**
     *
     * @param reallastupdate 订阅源的上次真正更新日期，用来与每条新闻的时间做对比，防止重复抓取相同的新闻
     * @param id 订阅源的id
     * @param url 订阅源的网址
     *
     * 该函数实现输出旧的新闻，并对变量初始化
     */
    public void init(Date reallastupdate, final long id, String url) {
        final long keepDateBorderTime = System.currentTimeMillis() - KEEP_TIME;
        keepDateBorder = new Date(keepDateBorderTime);// 设置旧的新闻保留时间
        if (reallastupdate == null) {
            reallastupdate = new Date(0);
        }
        this.realLastUpdate = reallastupdate.getTime();
        insertUpdate = reallastupdate;
        this.id = id;
        Uri feedEntiresUri = FeedContract.EntryColumns.CONTENT_URI(id);// ***feeds/id/entries代表一个订阅源的所有新闻

        /*final String query = new StringBuilder(// 查询语句
                FeedContract.EntryColumns.DATE).append('<')
                .append(keepDateBorderTime).append(" and ")
                .append(FeedContract.EntryColumns.FAVORITE).append("=0")
                .toString();
*/
        //context.getContentResolver().delete(feedEntiresUri, query, null);// 删除旧的并且没有收藏的新闻
        // 变量初始化
        feedRefreshed = false;
        title = null;
        dateStringBuilder = null;
        entryLink = null;
        description = null;
        entryDate = null;

        titleTagEntered = false;
        updatedTagEntered = false;
        linkTagEntered = false;
        descriptionTagEntered = false;
        pubDateTagEntered = false;
        dateTagEntered = false;
        lastUpdateDateTagEntered = false;
        authorTagEntered = false;
        author = null;
    }


    /**
     * 当遇到XML开始标签时调用该方法
     * @param uri 代表一个命名空间URI，如果元素没有命名空间则为null
     * @param localName 代表无前缀的本地名称
     * @param qName 代表有前缀的限定名
     * @param attributes 当前元素的属性
     *
     */
    @Override
    public void startElement(String uri, String localName, String qName,
                             Attributes attributes) throws SAXException {
        if (TAG_UPDATED.equals(localName)) {// updated
            updatedTagEntered = true;
            dateStringBuilder = new StringBuilder();
        } else if (TAG_ENTRY.equals(localName) || TAG_ITEM.equals(localName)) {// 遇到新闻入口
            // entry或item
            description = null;
            entryLink = null;
            if (!feedRefreshed) {// 更新标题
                ContentValues values = new ContentValues();

                if (title != null && title.length() > 0) {
                    values.put(FeedContract.FeedColumns.NAME, title
                            .toString().trim());
                }
                values.put(FeedContract.FeedColumns.LASTUPDATE,
                        System.currentTimeMillis() - 1000);
                context.getContentResolver().update(//// 更新‘更新日期'

                        FeedContract.FeedColumns.CONTENT_URI(id), values, null,// feeds/id
                        null);
                title = null;
                feedRefreshed = true;
            }
        } else if (TAG_TITLE.equals(localName)) {// 遇到新闻标题
            if (title == null) {
                titleTagEntered = true;
                title = new StringBuilder();
            }
        } else if (TAG_LINK.equals(localName)) { // 遇到新闻链接
            if (entryLink == null || qName.equals(localName)) {
                // 可能为空或者没有前缀,如<link  />
                entryLink = new StringBuilder();

                boolean foundLink = false;

                for (int n = 0, i = attributes.getLength(); n < i; n++) {
                    if (ATTRIBUTE_HREF.equals(attributes.getLocalName(n))) {
                        if (attributes.getValue(n) != null) {
                            entryLink.append(attributes.getValue(n));
                            foundLink = true;
                            linkTagEntered = false;
                        } else {
                            linkTagEntered = true;
                        }
                        break;
                    }
                }
                if (!foundLink) {
                    linkTagEntered = true;
                }
            }
        } else if ((TAG_DESCRIPTION.equals(localName) && !TAG_MEDIA_DESCRIPTION.equals(qName))
                || (TAG_CONTENT.equals(localName) && !TAG_MEDIA_CONTENT
                .equals(qName))) {//遇到正文
            descriptionTagEntered = true;
            description = new StringBuilder();
        } else if (TAG_SUMMARY.equals(localName)) { // 遇到新闻摘要
            if (description == null) {
                descriptionTagEntered = true;
                description = new StringBuilder();
            }
        } else if (TAG_PUBDATE.equals(localName)) { // 更新日期
            pubDateTagEntered = true;
            dateStringBuilder = new StringBuilder();
        } else if (TAG_DATE.equals(localName)) { // 更新日期
            dateTagEntered = true;
            dateStringBuilder = new StringBuilder();
        } else if (TAG_LASTBUILDDATE.equals(localName)) {// 上次发布日期
            lastUpdateDateTagEntered = true;
            dateStringBuilder = new StringBuilder();
        } else if (TAG_ENCODEDCONTENT.equals(localName)) {// 正文
            descriptionTagEntered = true;
            description = new StringBuilder();
        } else if (TAG_AUTHOR.equals(localName)//作者
                || TAG_CREATOR.equals(localName)) {
            authorTagEntered = true;
            if (TAG_CREATOR.equals(localName)) {//名称
                nameTagEntered = true;
            }
            if (author == null) {
                author = new StringBuilder();
            } else if (author.length() > 0) {
                // 可能包含多个作者
                author.append(COMMASPACE);
            }
        } else if (TAG_NAME.equals(localName)) {// 作者的name属性
            nameTagEntered = true;
        }
    }

    /**
     * 当遇到元素中的字符时调用此方法，即添加信息
     * @param ch 字符数组
     * @param start 字符数组的开始元素
     * @param length 字符数组的长度
     */
    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        if (titleTagEntered) {
            title.append(ch, start, length);
        } else if (updatedTagEntered) {
            dateStringBuilder.append(ch, start, length);
        } else if (linkTagEntered) {
            entryLink.append(ch, start, length);
        } else if (descriptionTagEntered) {
            description.append(ch, start, length);
        } else if (pubDateTagEntered) {
            dateStringBuilder.append(ch, start, length);
        } else if (dateTagEntered) {
            dateStringBuilder.append(ch, start, length);
        } else if (lastUpdateDateTagEntered) {
            dateStringBuilder.append(ch, start, length);
        } else if (authorTagEntered && nameTagEntered) {
            if(author!=null)
                author.append(ch, start, length);
        }
    }

    /**
     *
     * 遇到标签结束符时调用
     */
    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (TAG_TITLE.equals(localName)) {
            titleTagEntered = false;
        } else if ((TAG_DESCRIPTION.equals(localName) && !TAG_MEDIA_DESCRIPTION
                .equals(qName))
                || TAG_SUMMARY.equals(localName)
                || (TAG_CONTENT.equals(localName) && !TAG_MEDIA_CONTENT
                .equals(qName)) || TAG_ENCODEDCONTENT.equals(localName)) {
            descriptionTagEntered = false;
        } else if (TAG_LINK.equals(localName)) {
            linkTagEntered = false;
        } else if (TAG_UPDATED.equals(localName)) {
            entryDate = parseUpdateDate(dateStringBuilder.toString());
            updatedTagEntered = false;
        } else if (TAG_PUBDATE.equals(localName)) {
            entryDate = parsePubdateDate(dateStringBuilder.toString().replace(
                    TWOSPACE, SPACE));
            pubDateTagEntered = false;
        } else if (TAG_DATE.equals(localName)) {
            entryDate = parseUpdateDate(dateStringBuilder.toString());
            dateTagEntered = false;
        } else if (TAG_ENTRY.equals(localName) || TAG_ITEM.equals(localName)) {//遇到新闻结束符，更新数据库
            if (entryDate != null && entryDate.getTime() > (realLastUpdate)) {
                ContentValues values = new ContentValues();
                if (entryDate.after(insertUpdate)) {
                    insertUpdate = entryDate;

                    values.put(FeedContract.FeedColumns.REALLASTUPDATE,
                            insertUpdate.getTime());
                    context.getContentResolver().update(
                            FeedContract.FeedColumns.CONTENT_URI(id),
                            values, null, null);
                    values.clear();
                }

                values.put(FeedContract.EntryColumns.DATE,
                        entryDate.getTime());
                values.put(FeedContract.EntryColumns.TITLE,
                        unescapeString(title.toString().trim()));

                if (author != null) {
                    values.put(FeedContract.EntryColumns.AUTHOR,
                            unescapeString(author.toString()));
                }

                if (description != null) {
                    String descriptionString = description.toString().trim()
                            .replaceAll(HTML_SPAN_REGEX, EMPTY);

                    if (descriptionString.length() > 0) {

                        values.put(FeedContract.EntryColumns.ABSTRACT,
                                descriptionString);
                    }

                }
                values.put(FeedContract.EntryColumns.FAVORITE, 0);
                values.put(FeedContract.EntryColumns.ISREAD,0);
                context.getContentResolver()
                        .insert(FeedContract.EntryColumns.CONTENT_URI(id),
                                values);

                description = null;
                title = null;
                entryLink = null;
                author = null;
            } else if (TAG_RSS.equals(localName) || TAG_RDF.equals(localName)
                    || TAG_FEED.equals(localName)) {
            } else if (TAG_NAME.equals(localName)) {
                nameTagEntered = false;
            } else if (TAG_AUTHOR.equals(localName)
                    || TAG_CREATOR.equals(localName)) {
                authorTagEntered = false;

            }
        }
    }


    /**
     * 解析更新日期，将字符串转化为Date类型的时间
     * @param string 字符类型表示的时间格式
     * @return
     */
    private static Date parseUpdateDate(String string) {
        string = string.replace(Z, GMT);
        for (int n = 0; n < DATEFORMAT_COUNT; n++) {
            try {
                return UPDATE_DATEFORMATS[n].parse(string);
            } catch (ParseException e) {
            }
        }
        return null;
    }
    /**
     * 解析发布日期，将字符串转化为Date类型的时间
     * @param string 字符类型表示的时间格式
     * @return
     */
    private static Date parsePubdateDate(String string) {
        for (int n = 0; n < TIMEZONES_COUNT; n++) {
            string = string.replace(TIMEZONES[n], TIMEZONES_REPLACE[n]);
        }
        for (int n = 0; n < PUBDATEFORMAT_COUNT; n++) {
            try {
                return PUBDATE_DATEFORMATS[n].parse(string);
            } catch (ParseException e) {
            }
        }
        return null;
    }

    /**
     * 解码一些html符号为正常显示的符号，应用于标题和作者
     * @param str
     * @return
     */
    private static String unescapeString(String str) {
        String result = str.replace(AMP_SG, AMP)//将&amp;替换为&
                .replaceAll(HTML_TAG_REGEX, EMPTY).replace(HTML_LT, LT)//替换&lt;为<,*gt;为>
                .replace(HTML_GT, GT).replace(HTML_QUOT, QUOT)//替换&quot;为\
                .replace(HTML_APOS, APOSTROPHE).replaceAll("&nbsp;", " ");//替换&apos;为’

        if (result.indexOf(ANDRHOMBUS) > -1) {//将&#转义符替换
            return Html.fromHtml(result, null, null).toString();
        } else {
            return result;
        }
    }

}
