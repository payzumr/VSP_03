
package hawmetering;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse für webColor complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType name="webColor">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="red" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="green" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="blue" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="alpha" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "webColor", propOrder = {
    "red",
    "green",
    "blue",
    "alpha"
})
public class WebColor {

    protected int red;
    protected int green;
    protected int blue;
    protected int alpha;

    /**
     * Ruft den Wert der red-Eigenschaft ab.
     * 
     */
    public int getRed() {
        return red;
    }

    /**
     * Legt den Wert der red-Eigenschaft fest.
     * 
     */
    public void setRed(int value) {
        this.red = value;
    }

    /**
     * Ruft den Wert der green-Eigenschaft ab.
     * 
     */
    public int getGreen() {
        return green;
    }

    /**
     * Legt den Wert der green-Eigenschaft fest.
     * 
     */
    public void setGreen(int value) {
        this.green = value;
    }

    /**
     * Ruft den Wert der blue-Eigenschaft ab.
     * 
     */
    public int getBlue() {
        return blue;
    }

    /**
     * Legt den Wert der blue-Eigenschaft fest.
     * 
     */
    public void setBlue(int value) {
        this.blue = value;
    }

    /**
     * Ruft den Wert der alpha-Eigenschaft ab.
     * 
     */
    public int getAlpha() {
        return alpha;
    }

    /**
     * Legt den Wert der alpha-Eigenschaft fest.
     * 
     */
    public void setAlpha(int value) {
        this.alpha = value;
    }

}
