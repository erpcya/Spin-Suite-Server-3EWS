/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * This program is free software; you can redistribute it and/or modify it           *
 * under the terms version 2 of the GNU General Public License as published          *
 * by the Free Software Foundation. This program is distributed in the hope          *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied        *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                  *
 * See the GNU General Public License for more details.                              *
 * You should have received a copy of the GNU General Public License along           *
 * with this program; if not, write to the Free Software Foundation, Inc.,           *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                            *
 * For the text or an alternative of this public license, you may reach us           *
 * Copyright (C) 2012-2013 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Carlos Parada www.erpcya.com                    					 *
 *************************************************************************************/

package org.sfandroid.service;

import java.sql.SQLException;
import java.util.List;

import org.compiere.model.MWebServiceType;
import org.compiere.model.PO;
import org.compiere.model.X_WS_WebService_Para;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Login;

import org.sfandroid.model.MSFASyncMenu;
import org.sfandroid.model.MSFATable;

import com._3e.ADInterface.CompiereService;
import com.erpcya.DataRow;
import com.erpcya.ILCallDocument;
import com.erpcya.ILResponseDocument;
import com.erpcya.Query;
import com.erpcya.Response;
import com.erpcya.Values;

/**
 * 
 * @author <a href="mailto:carlosaparadam@gmail.com">Carlos Parada</a>
 *
 */
public class MSFAndroidServiceImpl {
	
	/**
	 * *** Constructor ***
	 * @author <a href="mailto:carlosaparadam@gmail.com">Carlos Parada</a> May 7, 2013, 11:21:46 PM
	 */
	public MSFAndroidServiceImpl() {
		// TODO Auto-generated constructor stub
		m_adempiere = new CompiereService();
		m_adempiere.connect();	
	}
	
	/**
	 * @author <a href="mailto:carlosaparadam@gmail.com">Carlos Parada</a> May 7, 2013, 11:21:54 PM
	 * @return
	 * @throws SQLException
	 * @return ILResponseDocument
	 * Initial Load Process
	 */
	public ILResponseDocument initialLoad(String p_WS_WebServiceValue) throws SQLException{
		ILResponseDocument resp  = ILResponseDocument.Factory.newInstance();
		//Get List of Items
		List<MSFASyncMenu> syncMenuItems = MSFASyncMenu.getNodes(0, p_WS_WebServiceValue);
		
		Response dataset =resp.addNewILResponse();
		for (MSFASyncMenu item:syncMenuItems){
			
			//Send Rule Before
			if (item.getAD_RuleBefore_ID()!=0){
				Query query = dataset.addNewQuery();
				query.setName(item.getName());
				query.setSQL(item.getAD_RuleBefore().getScript());
			}
			
			//Get Rule From Sync Table
			if(item.getSFA_Table_ID()!=0 && item.getWS_WebServiceType_ID()==0){
				if (item.getSFA_Table().getAD_Rule_ID()!=0){
					Query query = dataset.addNewQuery();
					query.setName(item.getName());
					query.setSQL(item.getSFA_Table().getAD_Rule().getScript());
				}
			}
			
			//Get Data From Web Service Type 
			else if (item.getSFA_Table_ID()!=0 && item.getWS_WebServiceType_ID()!=0)
				setDataFromTable(dataset,item);
			
			
			//Send Rule After
			if (item.getAD_RuleAfter_ID()!=0){
				Query query = dataset.addNewQuery();
				query.setName(item.getName());
				query.setSQL(item.getAD_RuleAfter().getScript());
			}
			
		}
		
		return resp;
		
	}
	
	/**
	 * Get Data From Table in Web Service
	 * @author <a href="mailto:carlosaparadam@gmail.com">Carlos Parada</a> 12/02/2014, 23:26:47
	 * @param res
	 * @return void
	 */
	private void setDataFromTable(Response resp,MSFASyncMenu sMenu)
	{
		MWebServiceType wst = new MWebServiceType(Env.getCtx(), sMenu.getWS_WebServiceType_ID(), null);
		X_WS_WebService_Para para = wst.getParameter("Action"); 
		//Set Query
		setSQLValues(para, sMenu, wst,resp);
	}
	
	/**
	 * 
	 * @author <a href="mailto:carlosaparadam@gmail.com">Carlos Parada</a> 22/02/2014, 11:31:49
	 * @param p_sql
	 * @param p_resp
	 * @param p_TableName
	 * @param p_columns
	 * @return void
	 */
	private void setValues(String p_sql, Response p_resp, String[] p_columns, MSFASyncMenu p_sMenu,String p_TableName){
		List<PO> records = new org.compiere.model.Query(Env.getCtx(), p_TableName, (p_sMenu.getWhereClause()==null ? "" : p_sMenu.getWhereClause()), null)
						.setOnlyActiveRecords(true)
						.list();
		
		for (PO record:records){
			Query query = p_resp.addNewQuery();
			query.setName(p_sMenu.getName());
			query.setSQL(p_sql);
			DataRow dr = query.addNewDataRow();
			for (int i=0 ; i < p_columns.length ; i++){
				Values values = dr.addNewValues();
				
				values.setValue(record.get_ValueAsString(p_columns[i]));
				
			}
		}
	}
	
	/**
	 * 
	 * @author <a href="mailto:carlosaparadam@gmail.com">Carlos Parada</a> 22/02/2014, 11:31:57
	 * @param p_Para
	 * @param p_sMenu
	 * @param p_Wst
	 * @param p_resp
	 * @return void
	 */
	private void setSQLValues(X_WS_WebService_Para p_Para,MSFASyncMenu p_sMenu,MWebServiceType p_Wst,Response p_resp){
		String sql= "";
		String values = "";
		MSFATable sfaTable = new MSFATable(Env.getCtx(), p_sMenu.getSFA_Table_ID(), null);//(MSFATable) sMenu.getSFA_Table();
		String[] columnsout = p_Wst.getOutputColumnNames(false);
		String[] columnsin = p_Wst.getInputColumnNames(false);
		String[] columnsSql = null;
		if (p_Para.getConstantValue().equals("Insert")){
			columnsSql = new String[columnsout.length];
			sql = "INSERT INTO " 
					+ sfaTable.getTableName() +" (";
			
			values = " VALUES (";
			for (int i=0 ;i<columnsout.length ;i++){
				sql+= columnsout[i] + ( i == columnsout.length-1 ? "" : "," );
				values += "?" + ( i == columnsout.length-1 ? "" : "," );
				columnsSql[i] = columnsout[i];
			}

			sql+= ") " + values + ");";
		}
		else if (p_Para.getConstantValue().equals("Update")){
			columnsSql = new String[columnsout.length+columnsin.length];
			
			sql = "UPDATE " + sfaTable.getTableName() + " SET " ;
			for (int i=0 ;i<columnsout.length ;i++){
				sql+= columnsout[i] + " = ? " + ( i == columnsout.length-1 ? "" : "," );
				columnsSql[i] = columnsout[i]; 
			}

			if (columnsin.length>0)
				sql+=" WHERE ";
			
			for (int i=0 ;i<columnsin.length ;i++){
				sql+= columnsin[i] + " = ? " + ( i == columnsin.length-1 ? "" : " AND " );
				columnsSql[i] = columnsin[columnsout.length + i];
			}
			sql+=";";
		}
		else if (p_Para.getConstantValue().equals("Delete")){
			columnsSql = new String[columnsin.length];

			sql = "DELETE FROM " + sfaTable.getTableName(); 

			if (columnsin.length>0)
				sql+=" WHERE ";
			
			for (int i=0 ;i<columnsin.length ;i++){
				sql+= columnsin[i] + " = ? " + ( i == columnsin.length-1 ? "" : " AND " );
				columnsSql[i] = columnsin[i];
			}
			
			sql+=";";
		}

		//Set Values For SQL
		setValues(sql,p_resp,columnsSql,p_sMenu,p_Wst.getAD_Table().getTableName());
	}
	
	/**
	 * 
	 * @author <a href="mailto:carlosaparadam@gmail.com">Carlos Parada</a> May 7, 2013, 9:34:36 PM
	 * @param input
	 * @return boolean
	 * Return Result of Logging In Adempiere
	 */
	protected boolean validateUser(ILCallDocument input)
	{
		com.erpcya.Login il = input.getILCall();
		return loggin(il.getUser(), il.getPassWord());
	}
	
	/**
	 * 
	 * @author <a href="mailto:carlosaparadam@gmail.com">Carlos Parada</a> May 7, 2013, 9:37:43 PM
	 * @param user
	 * @param pass
	 * @return
	 * @return boolean
	 * Logging In Adempiere
	 */
	private boolean loggin(String user, String pass)
	{
		boolean m_loggin =false; 
		Login loggin  = new Login(m_adempiere.getM_ctx()); 
		KeyNamePair[] users =	loggin.getRoles (user, pass);
		if (users!=null)
		{
			if(users.length>0)
			{
				m_loggin = true;
				KeyNamePair[] clients = loggin.getClients (users[0]);
				if (clients != null)
					m_AD_Client_ID = (clients.length > 0?(Integer)clients[0].getKey():null);
			}
			else
				m_loggin= false;
		}
		return m_loggin;
	}
	
	/** Compiere Service*/
	private CompiereService m_adempiere;
	/** Client ID*/
	private Integer m_AD_Client_ID;
	/** Logger*/
	private static CLogger	log = CLogger.getCLogger(SFAndroidServiceImpl.class);

	/**
	@SuppressWarnings("unused")
	public static void main(String[] args) {
		//org.compiere.Adempiere.startupEnvironment(true);
		//System.out.println(Env.getContext(Env.getCtx(), "AD_User_ID"));
		String a = new String();
		HashedMap m = new HashedMap();
		m.put("ab", new java.util.Date());
		m.put("abc", 1);
		m.put("abcd", null);
		
		String l_campo = "";
		int l_init=-1;
		int l_fin=-1;
		a="insert into prueba(ab,abc,abdc)values($ab$,$abc$,$abcd$)";
		while (a.indexOf("$")>0)
		{
			l_init=a.indexOf("$");
			l_fin=a.indexOf("$",l_init+1);
			l_campo = a.substring(l_init+1,l_fin);
			//a = a.substring(0,l_init ) + MAppDroidServicesImpl.transformValue(m.get(l_campo))+ a.substring(l_fin+1,a.length());
		
		}
		System.out.println(a);
		
	}
	**/
}
