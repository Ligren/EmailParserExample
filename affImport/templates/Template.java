package com.example.affImport.templates;

import com.expl.dblib.wsObj.v1_05.jobObj.JobObj;

public interface Template {
	
	boolean checkSubject(String msgSbj);
	
	void saveJob(JobObj jobObj) throws Exception;

}
