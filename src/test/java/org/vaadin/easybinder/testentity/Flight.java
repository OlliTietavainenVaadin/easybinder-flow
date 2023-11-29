package org.vaadin.easybinder.testentity;

import java.util.Date;

import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.group.GroupSequenceProvider;

@FlightValid
@GroupSequenceProvider(FlightGroupProvider.class)
public class Flight {
	@Valid
	FlightId flightId;

	@Temporal(TemporalType.TIMESTAMP)
	Date sbt;

	@Temporal(TemporalType.TIMESTAMP)
	Date ebt;

	@Temporal(TemporalType.TIMESTAMP)
	Date abt;

	@NotNull(groups = FlightGroupProvider.Scheduled.class, message = "Gate should be set when scheduled")
	String gate;
	
	boolean canceled;

	public Flight() {
		flightId = new FlightId();
	}

	/**
	 * @return the flightId
	 */
	public FlightId getFlightId() {
		return flightId;
	}

	/**
	 * @param flightId
	 *            the flightId to set
	 */
	public void setFlightId(FlightId flightId) {
		this.flightId = flightId;
	}

	/**
	 * @return the sbt
	 */
	public Date getSbt() {
		return sbt;
	}

	/**
	 * @return the ebt
	 */
	public Date getEbt() {
		return ebt;
	}

	/**
	 * @return the abt
	 */
	public Date getAbt() {
		return abt;
	}

	/**
	 * @return the gate
	 */
	public String getGate() {
		return gate;
	}

	/**
	 * @param sbt
	 *            the sbt to set
	 */
	public void setSbt(Date sbt) {
		this.sbt = sbt;
	}

	/**
	 * @param ebt
	 *            the ebt to set
	 */
	public void setEbt(Date ebt) {
		this.ebt = ebt;
	}

	/**
	 * @param abt
	 *            the abt to set
	 */
	public void setAbt(Date abt) {
		this.abt = abt;
	}

	/**
	 * @param gate
	 *            the gate to set
	 */
	public void setGate(String gate) {
		this.gate = gate;
	}
	
	public void setCanceled(boolean canceled) {
		this.canceled = canceled;
	}
	
	public boolean getCanceled() {
		return canceled;
	}

	public boolean isCanceled() {
		return canceled;
	}

}
