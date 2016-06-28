package istat.android.data.access;

public abstract class Data extends DOEntity {

	@Override
	public String getEntityName() {
		// TODO Auto-generated method stub
		return this.getClass().getCanonicalName();
	}

	@Override
	public String[] getEntityFieldNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getEntityPrimaryFieldName() {
		// TODO Auto-generated method stub
		return null;
	}

}
