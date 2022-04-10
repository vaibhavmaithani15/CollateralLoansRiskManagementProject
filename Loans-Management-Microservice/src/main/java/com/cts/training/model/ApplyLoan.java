package com.cts.training.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@Entity
@Table(name="applyLoan")
public class ApplyLoan {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@ApiModelProperty(value = "ApplicationId")
	private Integer appId;
	
	@ApiModelProperty(value = "CustomerId")
	private Integer custId;
	
	@ApiModelProperty(value = "Loan Amount")
	private Double loanAmnt;
	
	@ApiModelProperty(value = "Loan Tenure")
	private Integer tenure;
	
	@ApiModelProperty(value = "Collateral Details")
	@Column(columnDefinition = "varchar(200) default '-'")
	private String collDetails;
	
	@ApiModelProperty(value = "application status")
	@Column(columnDefinition = "varchar(30) default 'Waiting for Approval'")
	private String status;

}
