package org.sf.services;


import java.sql.SQLException;
import javax.xml.namespace.QName;
import org.codehaus.xfire.fault.XFireFault;
import com.erpconsultoresyasociados.DataSetSQLS;
import com.erpconsultoresyasociados.InitialLoadDocument;
import com.erpconsultoresyasociados.InitialLoadResponseDocument;

/**
 * @author carlos Parada
 *
 */
public class AppDroidServicesImpl extends MAppDroidServicesImpl implements AppDroidServices {

	/*
	 * @author Carlos
	 * @date 01/05/2012
	 * @time 14:04:32
	 * @type AppDroidServices
	 * @param
	 * @description
	 * @return
	 * @see org.mb.appdroid.AppDroidServices#initLoad(java.lang.String, java.lang.String)
	 */
	@Override
	public InitialLoadResponseDocument InitialLoad(InitialLoadDocument req)
			throws XFireFault {
		// TODO Auto-generated method stub
		
		InitialLoadResponseDocument resp;
		
		if (validateuser(req))
		{
			try {
				resp = initialLoad();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				resp = InitialLoadResponseDocument.Factory.newInstance();
				DataSetSQLS ds = resp.addNewInitialLoadResponse();
				ds.setError(e.getMessage());
				throw new XFireFault(e.getClass().toString() + " " + e.getMessage() , e.getCause(), new QName("initLoad"));
			}
		}
		else
		{
			resp = InitialLoadResponseDocument.Factory.newInstance();
			DataSetSQLS ds = resp.addNewInitialLoadResponse();
			ds.setError("Usuario o Contraseña incorrecta");
			throw new XFireFault("Usuario o Contraseña incorrecta",new Throwable("Usuario o Contraseña incorrecta"), new QName("authenticate"));
			
		}
				
		return resp;
	}
	
	
	/*
	 * @author Carlos Parada
	 * @date 01/05/2012
	 * @time 14:14:10
	 * @type AppDroidServices
	 * @param
	 * @description
	 * @return
	 * @see org.mb.appdroid.AppDroidServices#getVersion()
	 */
	@Override
	public String getVersion() {
		// TODO Auto-generated method stub
		return "1.0";
	}
}
