package gr.tsagi.jekyllforandroid.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Base64;
import android.util.Log;

import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import gr.tsagi.jekyllforandroid.data.PostsContract.CategoryEntry;
import gr.tsagi.jekyllforandroid.data.PostsContract.PostEntry;
import gr.tsagi.jekyllforandroid.data.PostsContract.TagEntry;


/**
 * Created by tsagi on 8/15/14.
 */
public class ParsePostData {

    private final String LOG_TAG = ParsePostData.class.getSimpleName();
    private final Context mContext;

    public ParsePostData(Context context) {
        mContext = context;
    }


    final String JK_TITLE = "title";
    final String JK_CATEGORY = "category";
    final String JK_TAGS = "tags";

    /**
     * Helper method to handle insertion of a new location in the weather database.
     *
     * @param tags the String array of tags for this post.
     */
    private void addTags(String tags, String id) {

        Log.d(LOG_TAG, "All Tags: " + tags);

        ContentValues tagValues = new ContentValues();

        // First, check if the post exists in the db
        Cursor cursorId = mContext.getContentResolver().query(
                TagEntry.CONTENT_URI,
                new String[]{TagEntry.COLUMN_POST_ID},
                TagEntry.COLUMN_POST_ID + " = ?",
                new String[]{id},
                null);

        Cursor cursorTags = mContext.getContentResolver().query(
                TagEntry.CONTENT_URI,
                new String[]{TagEntry.COLUMN_TAG},
                TagEntry.COLUMN_TAG + " = ?",
                new String[]{tags},
                null);

        if (cursorId.moveToFirst()) {
            cursorId.close();
            if (!cursorTags.moveToFirst()) {
                cursorTags.close();
                ContentValues updateValues = new ContentValues();
                updateValues.put(TagEntry.COLUMN_TAG, tags);
                if (updateValues.size() > 0) {
                    mContext.getContentResolver().update(TagEntry.CONTENT_URI, updateValues,
                            TagEntry.COLUMN_POST_ID + " = \"" + id + "\"", null);
                    Log.d(LOG_TAG, "Updated Tag Value.");
                } else {
                    Log.d(LOG_TAG, "No Values to insert.");
                }

            }
        } else {
            cursorId.close();
            cursorTags.close();
            tagValues.put(TagEntry.COLUMN_POST_ID, id);
            tagValues.put(TagEntry.COLUMN_TAG, tags);

            mContext.getContentResolver().insert(TagEntry.CONTENT_URI, tagValues);

            Log.d(LOG_TAG, "Inserted Tag Value.");

        }

    }

    /**
     * Helper method to handle insertion of a new location in the weather database.
     *
     * @param category The category name.
     * @return the row ID of the added location.
     */
    private void addCategory(String category, String id) {

        ContentValues tagValues = new ContentValues();

        // First, check if the post exists in the db
        Cursor cursorId = mContext.getContentResolver().query(
                CategoryEntry.CONTENT_URI,
                new String[]{CategoryEntry.COLUMN_POST_ID},
                CategoryEntry.COLUMN_POST_ID + " = ?",
                new String[]{id},
                null);

        Cursor cursorCategory = mContext.getContentResolver().query(
                CategoryEntry.CONTENT_URI,
                new String[]{CategoryEntry.COLUMN_CATEGORY},
                CategoryEntry.COLUMN_CATEGORY + " = ?",
                new String[]{category},
                null);

        if (cursorId.moveToFirst()) {
            cursorId.close();
            if (!cursorCategory.moveToFirst()) {
                cursorCategory.close();
                ContentValues updateValues = new ContentValues();
                updateValues.put(CategoryEntry.COLUMN_CATEGORY, category);
                if (updateValues.size() > 0) {
                    mContext.getContentResolver().update(CategoryEntry.CONTENT_URI, updateValues,
                            CategoryEntry.COLUMN_POST_ID + " = \"" + id + "\"", null);
                    Log.d(LOG_TAG, "Updated Category Value.");
                } else {
                    Log.d(LOG_TAG, "No Values to insert.");
                }

            }
        } else {
            cursorId.close();
            cursorCategory.close();
            tagValues.put(CategoryEntry.COLUMN_POST_ID, id);
            tagValues.put(CategoryEntry.COLUMN_CATEGORY, category);

            mContext.getContentResolver().insert(CategoryEntry.CONTENT_URI, tagValues);

            Log.d(LOG_TAG, "Inserted Category Value.");

        }

    }


    public ContentValues getDataFromContent(String id, String contentBytes, int type) {

        // If it is not specified if it is draft or not put it to drafts (drafts = 1)
        if (type > 1) {
            type = 1;
        }

        // Get and insert the new posts information into the database
        String postContent = null;

        // Blobs return with Base64 encoding so we have to UTF-8 them.
        byte[] bytes = Base64.decode(contentBytes, Base64.DEFAULT);
        try {
            postContent = new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        StringBuilder stringBuilder = new StringBuilder();

        InputStream is;
        BufferedReader r;

        is = new ByteArrayInputStream(postContent.getBytes());
        // read it with BufferedReader
        r = new BufferedReader(new InputStreamReader(is));
        String line;

        int yaml_dash = 0;
        String yamlStr = null;
        try {
            while ((line = r.readLine()) != null) {
                if (line.equals("---")) {
                    yaml_dash++;
                }
                if (yaml_dash < 2) {
                    if (!line.equals("---"))
                        yamlStr = yamlStr + line + "\n";
                }
                if (yaml_dash >= 2) {
                    if (!line.equals("---"))
                        if (line.equals(""))
                            stringBuilder.append("\n");
                        else
                            stringBuilder.append(line);
                }
            }
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        String content = stringBuilder.toString();

        Yaml yaml = new Yaml();

        HashMap<String, String[]> map = (HashMap<String, String[]>) yaml.load(yamlStr);
        HashMap<String, String> postmap = new HashMap<String, String>();

        postmap.put("title", String.valueOf(map.get("title")));
        postmap.put("category", String.valueOf(map.get("category")));
        postmap.put("tags", String.valueOf(map.get("tags")).replace("[", "").replace("]", ""));
        postmap.put("content", content);


        String title = postmap.get(JK_TITLE);
        String tags = postmap.get(JK_TAGS);
        String category = postmap.get(JK_CATEGORY);


        int i = id.indexOf('-', 1 + id.indexOf('-', 1 + id.indexOf('-')));
        long date = Long.parseLong(id.substring(0, i).replace("-", ""));

        addTags(tags, id);
        addCategory(category, id);

        // First, check if the post exists in the db
        Cursor cursorId = mContext.getContentResolver().query(
                PostEntry.CONTENT_URI,
                new String[]{PostEntry.COLUMN_POST_ID},
                PostEntry.COLUMN_POST_ID + " = ?",
                new String[]{id},
                null);

        Cursor cursorContent = mContext.getContentResolver().query(
                PostEntry.CONTENT_URI,
                new String[]{PostEntry.COLUMN_CONTENT},
                PostEntry.COLUMN_CONTENT + " = ?",
                new String[]{content},
                null);

        ContentValues postValues = new ContentValues();

        if (cursorId.moveToFirst()) {
            cursorId.close();
            if (!cursorContent.moveToFirst()) {
                cursorContent.close();
                ContentValues updateValues = new ContentValues();
                updateValues.put(PostEntry.COLUMN_CONTENT, content);
                if (updateValues.size() > 0) {
                    mContext.getContentResolver().update(PostEntry.CONTENT_URI, updateValues,
                            PostEntry.COLUMN_POST_ID + " = \"" + id + "\"", null);
                    Log.d(LOG_TAG, "Updated Value.");
                } else {
                    Log.d(LOG_TAG, "No Values to insert.");
                }

            }
        } else {
            cursorId.close();
            cursorContent.close();

            postValues.put(PostEntry.COLUMN_TITLE, title);
            postValues.put(PostEntry.COLUMN_DATETEXT, date);
            postValues.put(PostEntry.COLUMN_DRAFT, type);
            postValues.put(PostEntry.COLUMN_CONTENT, content);
            postValues.put(PostEntry.COLUMN_POST_ID, id);

        }

        return postValues;

    }

}
