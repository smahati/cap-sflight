package com.sap.cap.sflight.reviewer;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sap.cds.ql.Insert;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.mt.TenantInfo;
import com.sap.cds.services.mt.TenantProviderService;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.utils.dashboard.DashboardUtils;

import cds.gen.remotereviewservice.RemoteReviewService_;
import cds.gen.remotereviewservice.Review;
import cds.gen.remotereviewservice.ReviewContext;
import cds.gen.reviewservice.ReviewService_;
import cds.gen.reviewservice.TravelReview;

@Component
@ServiceName(RemoteReviewService_.CDS_NAME)
public class RemoteReviewHandler implements EventHandler {

	private final CqnService reviewService;

	@Autowired
	TenantProviderService tenantService;

	public RemoteReviewHandler(@Qualifier(ReviewService_.CDS_NAME) CqnService reviewService) {
		this.reviewService = reviewService;
	}

	@On
	public void onReview(ReviewContext context) {

		CdsRuntime cdsRuntime = context.getCdsRuntime();
		Review data = context.getData();
		DashboardUtils.create(cdsRuntime).consoleInfo("new review: " + data.toJson());

		TravelReview review = TravelReview.create();
		review.setComment(data.getComment());
		review.setEmail(data.getEmail());
		review.setRating(data.getRating());
		review.setTravelID(data.getTravelID());

		// apply to all tenants
		List<TenantInfo> tenants = tenantService.readTenantsInfo();
		if (tenants != null && !tenants.isEmpty()) {
			tenantService.readTenantsInfo().forEach(tenant -> {
				cdsRuntime.requestContext().modifyUser(u -> {u.setTenant(tenant.getTenant()); u.setName(review.getCreatedBy());}).run(req -> {
					cdsRuntime.changeSetContext().run(ch ->  {
						reviewService.run(Insert.into(ReviewService_.TRAVEL_REVIEW).entry(review));
					});
				});
			});
			// otherwise single tenant
		} else {
			cdsRuntime.requestContext().run(req -> {
				cdsRuntime.changeSetContext().run(ch ->  {
					reviewService.run(Insert.into(ReviewService_.TRAVEL_REVIEW).entry(review));
				});
			});
		}
	}

}
