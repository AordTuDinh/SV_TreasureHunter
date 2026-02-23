package ozudo.base.log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class slib_Properties {
    private File mFile;
    private Document mDoc;
    /*
     * Parsing xml file everytime we need a property is very slow. Therefore, we use a map to store all properties.
     */
    private Map<String, String> mProps = new HashMap<String, String>();

    /**
     * Creates a new instance of slib_Properties
     */
    public slib_Properties(String aFileName) throws Exception {
        this(new File(aFileName));
    }

    protected slib_Properties(File aFile) throws Exception {
        this.mFile = aFile;
        if (!aFile.exists()) {
            throw new IOException("File " + aFile.getName() + " is not exist.");
        }
        if (!aFile.canRead()) {
            throw new IOException("File " + aFile.getName() + " must be readable.");
        }

        // init document to parse xml file
        DocumentBuilderFactory docBuildFac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuildFac.newDocumentBuilder();
        mDoc = docBuilder.parse(mFile);

        // parse xml file to load properties
        loadConfig();
    }

    /*
     * @purpose: load all properties from config file by parsing xml file
     */
    private void loadConfig() {
        // Get root element
        Element root = mDoc.getDocumentElement();
        String rootName = root.getNodeName(); // "config"

        // get all elements
        NodeList nodeList = mDoc.getElementsByTagName("*");
        int nodeCount = nodeList.getLength();
        mProps.clear();

        for (int i = 0; i < nodeCount; i++) {
            Element element = (Element) nodeList.item(i);

            // get all child element of element(i)
            NodeList tmp = element.getElementsByTagName("*");
            int tmpLength = tmp.getLength();

            // if this element doesn't have child element then we will traverse down-top to get name of property.
            if (tmpLength == 0) {
                String nameProp = element.getNodeName();
                String value = element.getFirstChild().getNodeValue();
                // System.out.println(nameProp + " " + value);
                Element parentNode = null;
                while (true) {
                    parentNode = (Element) element.getParentNode();
                    if (parentNode.equals(root)) {
                        break;
                    }
                    nameProp = parentNode.getNodeName() + "." + nameProp;
                    element = parentNode;
                }
                nameProp = rootName + "." + nameProp;
                mProps.put(nameProp, value);
            }
        }
    }

    /*
     * @purpose: get a string value of a property
     *
     * @param: aKey - the name of property
     */
    public String getString(String aKey) {
        String value = mProps.get(aKey);
        return value;
    }

    /*
     * @purpose: get an integer value of a property
     *
     * @param: aKey - the name of property
     */
    public int getInt(String aKey) {
        String value = mProps.get(aKey);
        return Integer.decode(value).intValue();
    }

    /*
     * @purpose: get a boolean value of a property
     *
     * @param: aKey - the name of property
     */
    public boolean getBoolean(String aKey) {
        String value = mProps.get(aKey);
        return Boolean.parseBoolean(value);
    }

    /*
     * @purpose: get a string value of a property
     *
     * @param: aKey - the name of property
     */
    public void setString(String aKey, String aValue) {
        mProps.put(aKey, aValue);
    }
}
