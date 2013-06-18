package com.hbaspecto.pecas.sd;

import static org.junit.Assert.assertEquals;

import java.util.ResourceBundle;

import org.junit.Ignore;
import org.junit.Test;

import simpleorm.sessionjdbc.SSessionJdbc;

import com.hbaspecto.pecas.land.PostgreSQLLandInventory;
import com.hbaspecto.pecas.land.SimpleORMLandInventory;
import com.pb.common.util.ResourceUtil;

@Ignore
// TODO: Fix test
public class TestPossibleBugInSpaceTypesI {
	@Test
	public void testPossibleBug() {
		final ResourceBundle rb = ResourceUtil.getResourceBundle("sd");

		final String url = ResourceUtil.checkAndGetProperty(rb, "InputDatabase");
		final String user = ResourceUtil.checkAndGetProperty(rb,
				"InputDatabaseUser");
		final String password = ResourceUtil.checkAndGetProperty(rb,
				"InputDatabasePassword");

		final String driver = ResourceUtil.checkAndGetProperty(rb,
				"InputJDBCDriver");
		final String schema = null;

		SimpleORMLandInventory.prepareSimpleORMSession(rb);
		final SimpleORMLandInventory sormland = new PostgreSQLLandInventory();
		sormland.setDatabaseConnectionParameter(rb, driver, url, user, password,
				schema);

		final SSessionJdbc session = SSessionJdbc.getThreadLocalSession();

		final double from3to5 = SpaceTypesI
				.getAlreadyCreatedSpaceTypeBySpaceTypeID(3).getTransitionConstantTo(
						session, 5);
		assertEquals(122, from3to5, 0.1);
		final double from3to3 = SpaceTypesI
				.getAlreadyCreatedSpaceTypeBySpaceTypeID(3).getTransitionConstantFrom(
						session, 3);
		assertEquals(-86.3, from3to3, 0.1);
	}
}
