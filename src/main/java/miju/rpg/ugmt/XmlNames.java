package miju.rpg.ugmt;

/**
 * Collect important Xml element names and attributes.
 */
public final class XmlNames {
    /** Property for XML Factories. */
    public static final String YES = "yes";

    /** Hide constructor. */
    private XmlNames() {
    }

    /** Element constants. */
    public final class Elements {
        public static final String TAGGED = "tagged";
        public static final String TAG = "tag";
        public static final String DATA = "data";
        public static final String ATTRIBUTE = "attribute";
        public static final String SKILL = "skill";
        public static final String EQUIP = "equipment";
        public static final String SPELL = "spell";

        /** Hide constructor. */
        private Elements() {
        }
    }

    /** Attribute constants. */
    public final class Attributes {
        public static final String CHAIN = "chain";
        public static final String NAME = "name";
        public static final String ID = "id";
        public static final String PERMIT = "permit";
        public static final String MATERIAL = "material";        
        public static final String FILE = "file";
        public static final String VALUE = "value";
        public static final String TYPE = "type";

        /** Hide constructor. */
        private Attributes() {
        }
    }
}
