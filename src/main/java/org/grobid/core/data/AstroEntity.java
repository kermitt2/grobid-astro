package org.grobid.core.data;

import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.lexicon.AstroLexicon;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;

import java.util.ArrayList;
import java.util.List;

/**
 *  Representation of a mention of an astronomical entity.
 *
 */
public class AstroEntity implements Comparable<AstroEntity> {   
	
	// Orign of the entity definition
	public enum Origin {
		GROBID	("grobid"),
		USER	("user");
		
		private String name;

		private Origin(String name) {
          	this.name = name;
		}

		public String getName() {
			return name;
		}
	};
	
	// surface form of the entity as it appears in the source document
	private String rawForm = null;
	
	// list of layout tokens corresponding to the mention in the source document
	private List<LayoutToken> tokens = null;
	
	// normalized form of the entity
    private String normalizedForm = null;
	
	// type of the entity
	private AstroLexicon.Astro_Type type = null;
	
	// Entity identifier if the mention has been solved/disambiguated against the
	// knowledge base of astronomical entities. The identifier is the unique 
	// identifier of the entity in this KB.
	private String entityId = null;
	
	// relative offset positions in context, if defined
	private OffsetPosition offsets = null;
	
	// confidence score of the entity in context, if defined
	private double conf = 0.8;
	
	// optional bounding box in the source document
	private BoundingBox box = null;
		
	// orign of the entity definition
	private Origin origin = Origin.GROBID;
	
    public AstroEntity() {
		this.offsets = new OffsetPosition();
    }
	
	public AstroEntity(String rawForm) {
        this.rawForm = rawForm;
		this.offsets = new OffsetPosition();
    }

	public AstroEntity(AstroEntity ent) {
		rawForm = ent.rawForm;
		normalizedForm = ent.normalizedForm;
		type = ent.type;
		offsets = ent.offsets;
		conf = ent.conf;
		origin = ent.origin;
	}

    public String getRawForm() {
        return rawForm;
    }
	
	public void setRawForm(String raw) {
        this.rawForm = raw;
    }

	public String getNormalizedForm() {
        return normalizedForm;
    }
	
	public void setNormalizedForm(String normalized) {
        this.normalizedForm = normalized;
    }

	public AstroLexicon.Astro_Type getType() {
		return type;
	}
	
	public String getEntityId() {
		return entityId;
	}
	
	public void setEntityId(String id) {
		this.entityId = id;
	}
	
	public void setType(AstroLexicon.Astro_Type theType) {
		type = theType;
	}

	public OffsetPosition getOffsets() {
		return offsets;
	}
	
	public void setOffsets(OffsetPosition offsets) {
		this.offsets = offsets;
	}
	
	public void setOffsetStart(int start) {
        offsets.start = start;
    }

    public int getOffsetStart() {
        return offsets.start;
    }

    public void setOffsetEnd(int end) {
        offsets.end = end;
    }

    public int getOffsetEnd() {
        return offsets.end;
    }
	
	public double getConf() {
		return this.conf;
	}
	
	public void setConf(double conf) {
		this.conf = conf;
	}
	
	public Origin getOrigin() {
		return origin;
	}
	
	public void setOrigin(Origin origin) {
		this.origin = origin;
	}
	
	public void normalize() {
		// TBD is necessary
	}
	
	@Override
	public boolean equals(Object object) {
		boolean result = false;
		if ( (object != null) && object instanceof AstroEntity) {
			int start = ((AstroEntity)object).getOffsetStart();
			int end = ((AstroEntity)object).getOffsetEnd();
			if ( (start == offsets.start) && (end == offsets.end) ) {
				result = true;
			}
		}
		return result;
	}

	@Override
	public int compareTo(AstroEntity theEntity) {
		int start = theEntity.getOffsetStart();
		int end = theEntity.getOffsetEnd();
		
		if (offsets.start != start) 
			return offsets.start - start;
		else 
			return offsets.end - end;
	}
	
	public String toJson() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("{ ");
		buffer.append("\"rawForm\" : \"" + rawForm + "\"");
		if (normalizedForm != null)
			buffer.append(", \"normalizedForm\" : \"" + normalizedForm + "\"");
		if (type != null)
			buffer.append(", \"type\" : \"" + type.getName() + "\"");	
		if (entityId != null)
			buffer.append(", \"id\" : \"" + entityId + "\"");	
		
		buffer.append(", \"offsetStart\" : " + offsets.start);
		buffer.append(", \"offsetEnd\" : " + offsets.end);	
		
		buffer.append(", \"conf\" : \"" + conf + "\"");
		
		buffer.append(" }");
		return buffer.toString();
	}
	
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        if (rawForm != null) {
			buffer.append(rawForm + "\t");
		}
		if (normalizedForm != null) {
			buffer.append(normalizedForm + "\t");
		}
		if (type != null) {
			buffer.append(type + "\t");	
		}
		if (entityId != null)
			buffer.append(entityId + "\t");	
		
		if (offsets != null) {
			buffer.append(offsets.toString() + "\t");
		}
		
        return buffer.toString();
    }
	
	/** 
	 * Export of entity annotation in TEI standoff format 
	 */	 
	public String toTEI(String id, int n) {
		StringBuffer buffer = new StringBuffer();
		// tbd if necessary
		return buffer.toString();
	}
}