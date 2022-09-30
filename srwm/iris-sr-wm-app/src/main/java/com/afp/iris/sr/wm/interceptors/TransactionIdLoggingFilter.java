package com.afp.iris.sr.wm.interceptors;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.afp.iris.sr.wm.config.monitoring.SoftConfigurations;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.MessageFormat;

@Component
public class TransactionIdLoggingFilter extends OncePerRequestFilter {
	@Autowired
	SoftConfigurations softConfigurations;
	private static final String HEADER_X_AFP_TRANSACTION_ID = "X-AFP-TRANSACTION-ID";
	private String X_AFP_TRANSACTION_ID = "WebManager-{0}-xxxx-{1}";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		if (request.getHeader(HEADER_X_AFP_TRANSACTION_ID) != null
				&& !request.getHeader(HEADER_X_AFP_TRANSACTION_ID).isEmpty()) {
			MDC.put(HEADER_X_AFP_TRANSACTION_ID, String.valueOf(request.getHeader(HEADER_X_AFP_TRANSACTION_ID)));
		} else {
			MDC.put(HEADER_X_AFP_TRANSACTION_ID, generateTransactionId());
		}

		filterChain.doFilter(request, response);

		if (MDC.get(HEADER_X_AFP_TRANSACTION_ID) != null) {
			MDC.remove(HEADER_X_AFP_TRANSACTION_ID);
		}

	}

	private String generateTransactionId() {
		return MessageFormat.format(X_AFP_TRANSACTION_ID, softConfigurations.getVersion(),
				Long.toString(System.currentTimeMillis()));
	}

}