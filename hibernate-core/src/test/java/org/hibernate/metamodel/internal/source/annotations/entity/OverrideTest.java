package org.hibernate.metamodel.internal.source.annotations.entity;

import java.util.Date;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.junit.Test;

import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.relational.Table;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.testing.junit4.BaseAnnotationBindingTestCase;
import org.hibernate.testing.junit4.Resources;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class OverrideTest extends BaseAnnotationBindingTestCase {

	@javax.persistence.MappedSuperclass
	@AttributeOverrides({
			@AttributeOverride(name = "shipment.country", column = @Column(name = "S_COUNTRY")),
			@AttributeOverride(name = "shipment.zip", column = @Column(name = "S_ZIP")),
			@AttributeOverride(name = "shipment.details.city", column = @Column(name = "S_CITY")),
			@AttributeOverride(name = "shipment.details.street", column = @Column(name = "S_STREET"))
	})
	public abstract class AbstractOrder {
		@Temporal(TemporalType.TIMESTAMP)
		Date created;
	}


	@Entity
	public class Order extends AbstractOrder {
		@Id
		long id;

		@Embedded
		Address shipment;

		@Embedded
		@AttributeOverrides({
				@AttributeOverride(name = "country", column = @Column(name = "P_COUNTRY")),
				@AttributeOverride(name = "zip", column = @Column(name = "P_ZIP")),
				@AttributeOverride(name = "details.city", column = @Column(name = "P_CITY")),
				@AttributeOverride(name = "details.street", column = @Column(name = "P_STREET"))
		})
		Address payment;

	}

	@Embeddable
	public class Address {
		String country;
		String zip;
		@Embedded
		AddressDetails details;


	}

	@Embeddable
	public class AddressDetails {
		String city;
		String street;
	}

	@Test
	@Resources(annotatedClasses = { AbstractOrder.class, Order.class, Address.class, AddressDetails.class })
	public void testAttributeOverrides() {
		EntityBinding entityBinding = getEntityBinding( Order.class );
		Table table = (Table) entityBinding.getPrimaryTable();
		for(Value value : table.values()){
			System.out.println(((org.hibernate.metamodel.spi.relational.Column)value).getColumnName().getText());
		}

	}
}
