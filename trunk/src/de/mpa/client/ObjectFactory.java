
package de.mpa.client;

import javax.xml.bind.annotation.XmlRegistry;

import de.mpa.client.settings.DbSearchSettings;
import de.mpa.client.settings.DenovoSearchSettings;
import de.mpa.client.settings.SearchSettings;
import de.mpa.client.settings.SpecSimSettings;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the de.mpa.client package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: de.mpa.client
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link SearchSettings }
     * 
     */
    public SearchSettings createSearchSettings() {
        return new SearchSettings();
    }

    /**
     * Create an instance of {@link DenovoSearchSettings }
     * 
     */
    public DenovoSearchSettings createDenovoSearchSettings() {
        return new DenovoSearchSettings();
    }

    /**
     * Create an instance of {@link DbSearchSettings }
     * 
     */
    public DbSearchSettings createDbSearchSettings() {
        return new DbSearchSettings();
    }

    /**
     * Create an instance of {@link SpecSimSettings }
     * 
     */
    public SpecSimSettings createSpecSimSettings() {
        return new SpecSimSettings();
    }

}