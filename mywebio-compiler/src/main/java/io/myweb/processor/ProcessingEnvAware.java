package io.myweb.processor;

import javax.annotation.processing.ProcessingEnvironment;

public class ProcessingEnvAware extends AnnotationMessagerAware {

	private final ProcessingEnvironment processingEnvironment;

	public ProcessingEnvAware(ProcessingEnvironment processingEnvironment) {
		super(processingEnvironment.getMessager());
		this.processingEnvironment = processingEnvironment;
	}

	public ProcessingEnvironment getProcessingEnv() {
		return processingEnvironment;
	}
}
