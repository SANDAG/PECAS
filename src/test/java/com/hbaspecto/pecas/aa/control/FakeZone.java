package com.hbaspecto.pecas.aa.control;

import com.hbaspecto.pecas.zones.AbstractZone;

public class FakeZone extends AbstractZone {
	int _userNumber;

	protected FakeZone(int i, int userNumber) {
		super(i, userNumber);
		_userNumber = userNumber;
	}

	@Override
	public int getZoneUserNumber() {
		return _userNumber;
	}

}
