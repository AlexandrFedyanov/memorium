package com.cacao.libgdxgame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class LanguagesManager {
    private static final String DEFAULT_LANGUAGE = "en_UK";
    private static final String LANGUAGES_FILE = "data/languages.xml";
    private static LanguagesManager _instance = null;
    private final HashMap<String, String> _language = new HashMap<>();
    private String _languageName;

    private LanguagesManager() {
        Locale locale = Locale.getDefault();
        _languageName = locale.getLanguage() + "_" + locale.getCountry();
        if (!loadLanguage(_languageName)) {
            // Fall back to any entry with the same language code (e.g. ru_BY -> ru_RU).
            if (!loadLanguage(locale.getLanguage() + "_")) {
                loadLanguage(DEFAULT_LANGUAGE);
                _languageName = DEFAULT_LANGUAGE;
            }
        }
    }

    public static LanguagesManager getInstance() {
        if (_instance == null) {
            _instance = new LanguagesManager();
        }
        return _instance;
    }

    public String getLanguage() {
        return _languageName;
    }

    public String getString(String key) {
        String string = _language.get(key);
        return string == null ? key : string;
    }

    public String getString(String key, Object... args) {
        return String.format(getString(key), args);
    }

    /** Loads the language whose name equals {@code languageName}, or starts with it when it ends in '_'. */
    public boolean loadLanguage(String languageName) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            FileHandle fileHandle = Gdx.files.internal(LANGUAGES_FILE);
            Document doc = db.parse(fileHandle.read());
            Element root = doc.getDocumentElement();
            NodeList languages = root.getElementsByTagName("language");
            for (int i = 0; i < languages.getLength(); i++) {
                Node language = languages.item(i);
                String name = language.getAttributes().getNamedItem("name").getTextContent();
                boolean matches = languageName.endsWith("_") ? name.startsWith(languageName) : name.equals(languageName);
                if (matches) {
                    _language.clear();
                    NodeList strings = ((Element) language).getElementsByTagName("string");
                    for (int j = 0; j < strings.getLength(); j++) {
                        NamedNodeMap attributes = strings.item(j).getAttributes();
                        String key = attributes.getNamedItem("key").getTextContent();
                        String value = attributes.getNamedItem("value").getTextContent();
                        _language.put(key, value.replace("<br />", "\n"));
                    }
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            Gdx.app.error("LanguagesManager", "Error loading languages file " + LANGUAGES_FILE, e);
            return false;
        }
    }
}
