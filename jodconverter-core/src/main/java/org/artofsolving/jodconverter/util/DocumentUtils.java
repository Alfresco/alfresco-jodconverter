package org.artofsolving.jodconverter.util;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.lang.XComponent;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextField;
import com.sun.star.text.XTextFieldsSupplier;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.UnoRuntime;

import org.artofsolving.jodconverter.office.OfficeException;

public class DocumentUtils
{
    private static final String FILENAME_FIELD = "file name";

    public static void removeFilenameFields(XComponent document) throws OfficeException
    {
        try
        {
            XTextDocument xTextDoc = UnoRuntime.queryInterface(XTextDocument.class, document);

            XTextFieldsSupplier fieldsSupplier = UnoRuntime.queryInterface(XTextFieldsSupplier.class, xTextDoc);
            XEnumerationAccess xFieldsAccess = fieldsSupplier.getTextFields();
            XEnumeration xEnumeration = xFieldsAccess.createEnumeration();
            while (xEnumeration.hasMoreElements())
            {
                Object fieldObj = xEnumeration.nextElement();
                XTextField field = UnoRuntime.queryInterface(XTextField.class, fieldObj);

                String fieldType = field.getPresentation(true);

                if (fieldType.toLowerCase().contains(FILENAME_FIELD))
                {
                    String cachedValue = field.getPresentation(false);
                    XTextRange textRange = field.getAnchor();
                    textRange.setString(cachedValue);
                }
            }
        }
        catch (NoSuchElementException e)
        {
            // No filename fields found, nothing to remove
        }
        catch (Exception e)
        {
            // No filename fields found, nothing to remove
        }
    }
}
