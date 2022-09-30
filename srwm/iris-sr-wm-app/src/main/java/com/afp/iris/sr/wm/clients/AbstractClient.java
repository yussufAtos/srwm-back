package com.afp.iris.sr.wm.clients;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;

import com.afp.iris.sr.wm.config.monitoring.SoftConfigurations;

import java.text.MessageFormat;

public abstract class AbstractClient {
	@Autowired SoftConfigurations softConfigurations;
	protected static final String LOG_REQUEST_WITH_URL_RESPONSE = "{} {} with URL {} -- response {}";
	protected static final String X_AFP_TRANSACTION_ID = "WebManager-{0}-xxxx-{1}";
	protected static final String HEADER_X_AFP_TRANSACTION_ID = "X-AFP-TRANSACTION-ID";
	protected static final String HEADER_X_AFP_DOCUMENT_ETag = "X-AFP-Document-ETag";
	protected static final String HEADER_X_AFP_DOCUMENT_GLOBAL_ETAG = "X-AFP-DOCUMENT-GLOBAL-ETAG";
	protected static final String AUTHENTICATE_ERROR_MESSAGE_FORMAT = "Unable to authenticate to CMS. Reason : status code={} message={}";
	protected static final String CONTENT_TYPE_DOCUMENT_NEWSML = "application/vnd.afp.iptcg2newsmessage+xml ; charset=utf-8";
	protected static final String CONTENT_TYPE_VALIDATION_ORDERS = "application/vnd.afp.iris.sr.validation-orders+xml";
	protected static final String DEFAULT_XML_PRODUCTS = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><validationOrders xmlns=\"http://sr.iris.afp.com/request\" xmlns:ns2=\"http://sr.iris.afp.com/common\" xmlns:ns3=\"http://sr.iris.afp.com/rubric\"><product href=\"http://products.afp.com/wires/AFP-FORUM\"/><product href=\"http://products.afp.com/wires/AFP-NO-PUSH\"/></validationOrders>";
	protected static final String DEPUBLISH_XML_PRODUCTS = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><validationOrders xmlns=\"http://sr.iris.afp.com/request\" xmlns:ns2=\"http://sr.iris.afp.com/common\" xmlns:ns3=\"http://sr.iris.afp.com/rubric\"><product href=\"http://products.afp.com/wires/AFP-NO-PUSH\"/></validationOrders>";
	protected static final String X_AFP_LOCK_FORCED = "X-AFP-Lock-Forced";
	protected static final String CMS = "CMS";
	protected static final String SCOM = "SCOM";
    protected static final String COOKIE_NAME = "srwm";

}
