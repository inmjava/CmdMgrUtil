package com.copel.javaprocedurecmd;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.copel.cmdutil.ResultSet;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class JavaProcedureCmdMgr extends CmdMgrBase {

	private static final String PATH = null;

	public void addAddreessMultipleUsers(String sUserGroup, String sWebAddress) {

		// get all users in the group
		ResultSet lUsersSet = executeCapture("LIST MEMBERS FOR USER GROUP \"" + sUserGroup + "\";");

		lUsersSet.moveFirst();
		while (!lUsersSet.isEof()) {
			ResultSet member = (ResultSet) lUsersSet.getFieldValue(DisplayPropertyEnum.MEMBER_RESULTSET);
			member.moveFirst();
			// go through each user and add address for them
			while (!member.isEof()) {
				String lUserName = member.getFieldValueString(DisplayPropertyEnum.LOGIN);
				String lEmailAddress = lUserName + "@" + sWebAddress;
				int lResult = execute("ADD ADDRESS \"Email Address\" " + "PHYSICALADDRESS \"" + lEmailAddress + "\" " + "DELIVERYTYPE EMAIL " + "DEVICE \"Generic Email\" " + "TO USER \"" + lUserName + "\" ; ");

				if (lResult == ErrorHandler.EXIT_CODE_SUCCESS) {
					printOut("Address succesfully added to: " + lUserName);
				} else {
					// something wrong with adding address process
					printErr("Failed to add address to: " + lUserName);
				}
				member.moveNext();
			}
			lUsersSet.moveNext();
		}

	}

	public void criarProjetoPadraoCopel(String oi, String nomeProjeto, String descricaoProjeto) {
		// String nomeProjeto = "ING - Gestão de Indicadores";
		// String descricaoProjeto =
		// "Aplicativo BI para análise e acompanhamento dos indicadores estratégicos da Copel";
		final boolean isOracle = true;
		String passwd = "changeme";
		String siglaNomeProjeto = nomeProjeto.substring(0, 3);

		// Delete everything
		execute("DELETE PROJECT \"" + nomeProjeto + "\";");
		execute("DELETE DBINSTANCE \"Warehouse - " + nomeProjeto + "\";");
		execute("DELETE DBCONNECTION \"DC - " + nomeProjeto + "\";");
		execute("DELETE DBLOGIN \"Login - " + nomeProjeto + "\";");
		execute("DELETE USER GROUP \"" + siglaNomeProjeto + "\";");
		execute("DELETE USER GROUP \"" + siglaNomeProjeto + "-ADM\";");
		execute("DELETE USER GROUP \"" + siglaNomeProjeto + "-DSV\";");
		execute("DELETE USER GROUP \"" + siglaNomeProjeto + "-MBL\";");
		execute("DELETE USER GROUP \"" + siglaNomeProjeto + "-WEB\";");

		// Criando o projeto
		execute("CREATE PROJECT \"" + nomeProjeto + "\" DESCRIPTION \"" + descricaoProjeto + "\" ENABLEGUESTACCOUNT FALSE;");

		if (isOracle) {
			execute("CREATE DBLOGIN \"Login - " + nomeProjeto + "\" LOGIN \"adm" + siglaNomeProjeto.toLowerCase() + "\" PASSWORD \"" + passwd + "\";");
			execute("CREATE DBCONNECTION \"DC - " + nomeProjeto + "\" ODBCDSN \"DW11gr2\"  DEFAULTLOGIN \"Login - " + nomeProjeto + "\";");
			execute("CREATE DBINSTANCE \"Warehouse - " + nomeProjeto + "\" DBCONNTYPE \"Oracle 11gR2\" DBCONNECTION \"DC - " + nomeProjeto + "\" HIGHTHREADS 2 MEDIUMTHREADS 2 LOWTHREADS 10;");
			execute("ADD DBINSTANCE \"Warehouse - " + nomeProjeto + "\" IN PROJECT \" " + nomeProjeto + "\";");
		}

		execute("ALTER PROJECT CONFIGURATION USEWHLOGINEXEC FALSE IN PROJECT \"" + nomeProjeto + "\";");

		// Removendo permissão Everyone
		execute("REVOKE SECURITY ROLE \"Usuários Normais\" FROM GROUP \"Everyone\" FROM PROJECT \"" + nomeProjeto + "\";");
		execute("REVOKE SECURITY ROLE \"Normal Users\" FROM GROUP \"Everyone\" FROM PROJECT \"" + nomeProjeto + "\";");

		// Adicionando definições de tabela de estatística
		execute("ALTER STATISTICS DBINSTANCE \"Warehouse - Enterprise Manager\" " + "BASICSTATS ENABLED " + "DETAILEDREPJOBS TRUE " + "DETAILEDDOCJOBS TRUE " + "JOBSQL TRUE " + "COLUMNSTABLES TRUE " + "PROMPTANSWERS TRUE " + "SUBSCRIPTIONDELIVERIES TRUE " + "IN PROJECT \"" + nomeProjeto + "\";");

		execute("CREATE USER GROUP \"" + siglaNomeProjeto + "\" DESCRIPTION \"Agrupa tipos diferentes de permissões de usuários para o projeto " + nomeProjeto + "\"  IN GROUP \"PROJ\";");

		execute("CREATE USER GROUP \"" + siglaNomeProjeto + "-ADM\" DESCRIPTION \"Agupa usuários principais, com permissão de adicionar usuários em grupos, para o projeto " + nomeProjeto + "\" IN GROUP \"" + siglaNomeProjeto + "\";");
		execute("ALTER USER GROUP \"" + siglaNomeProjeto + "-ADM\" GROUP \"USERADM\";");

		execute("CREATE USER GROUP \"" + siglaNomeProjeto + "-DSV\" DESCRIPTION \"Agupa desenvolvedores para o projeto " + nomeProjeto + "\" IN GROUP \"" + siglaNomeProjeto + "\";");
		execute("GRANT SECURITY ROLE \"Desenvolvedor\" TO GROUP \"" + siglaNomeProjeto + "-DSV\" FOR PROJECT \"" + nomeProjeto + "\";");
		execute("ALTER USER GROUP \"DESENV\" GROUP \"" + siglaNomeProjeto + "-DSV\";");

		execute("CREATE USER GROUP \"" + siglaNomeProjeto + "-MBL\" DESCRIPTION \"Agupa  usuários finais com permissão mobile para o projeto " + nomeProjeto + "\" IN GROUP \"" + siglaNomeProjeto + "\";");
		execute("GRANT SECURITY ROLE \"Mobile\" TO GROUP \"" + siglaNomeProjeto + "-MBL\" FOR PROJECT \"" + nomeProjeto + "\";");

		execute("CREATE USER GROUP \"" + siglaNomeProjeto + "-WEB\" DESCRIPTION \"Agupa  usuários finais com permissão web para o projeto " + nomeProjeto + "\" IN GROUP \"" + siglaNomeProjeto + "\";");
		execute("GRANT SECURITY ROLE \"Web\" TO GROUP \"" + siglaNomeProjeto + "-WEB\" FOR PROJECT \"" + nomeProjeto + "\";");

		execute("GRANT SECURITY ROLE \"Arquiteto\" TO GROUP \"ARQ\" FOR PROJECT \"" + nomeProjeto + "\";");

		// Recebe o resultado da consulta
		ResultSet aclGroupRS = executeCapture("LIST PROPERTIES FOR ACL FROM GROUP \"" + siglaNomeProjeto + "\";");

		// Verifica se existe algum resultado
		if (aclGroupRS.getRowCount() > 0) {

			// Anda pelo primeiro elemento do ResultSet
			// aclGroupRS
			aclGroupRS.moveFirst();
			while (!aclGroupRS.isEof()) {

				// INÍCIO - Implementação sobre o resultset
				// aclGroupRS
				String trusteeName = aclGroupRS.getFieldValueString(TRUSTEE_NAME);
				String pattern = "(.*)(\\(+)(\\w+)(\\)?)";
				trusteeName = trusteeName.replaceAll(pattern, "$3");
				String trusteeType = aclGroupRS.getFieldValueString(TRUSTEE_TYPE).equals("User Group") ? "GROUP" : "USER";

				execute("REMOVE ACE FROM GROUP \"" + siglaNomeProjeto + "\" " + trusteeType + " \"" + trusteeName + "\";");
				execute("REMOVE ACE FROM GROUP \"" + siglaNomeProjeto + "-ADM\" " + trusteeType + " \"" + trusteeName + "\";");
				execute("REMOVE ACE FROM GROUP \"" + siglaNomeProjeto + "-DSV\" " + trusteeType + " \"" + trusteeName + "\";");
				execute("REMOVE ACE FROM GROUP \"" + siglaNomeProjeto + "-MBL\" " + trusteeType + " \"" + trusteeName + "\";");
				execute("REMOVE ACE FROM GROUP \"" + siglaNomeProjeto + "-WEB\" " + trusteeType + " \"" + trusteeName + "\";");
				// FIM - Implementação sobre o resultset
				// aclGroupRS

				// Da continuidade a iteração com o ResultSet
				// aclGroupRS
				aclGroupRS.moveNext();
			}

			execute("REMOVE ACE FROM GROUP \"" + siglaNomeProjeto + "\" GROUP \"System Monitors\";");
			execute("REMOVE ACE FROM GROUP \"" + siglaNomeProjeto + "-ADM\" GROUP \"System Monitors\";");
			execute("REMOVE ACE FROM GROUP \"" + siglaNomeProjeto + "-DSV\" GROUP \"System Monitors\";");
			execute("REMOVE ACE FROM GROUP \"" + siglaNomeProjeto + "-MBL\" GROUP \"System Monitors\";");
			execute("REMOVE ACE FROM GROUP \"" + siglaNomeProjeto + "-WEB\" GROUP \"System Monitors\";");

			execute("ADD ACE FOR GROUP \"" + siglaNomeProjeto + "\" GROUP \"" + siglaNomeProjeto + "-ADM\" ACCESSRIGHTS MODIFY;");
			execute("ADD ACE FOR GROUP \"" + siglaNomeProjeto + "-ADM\" GROUP \"" + siglaNomeProjeto + "-ADM\" ACCESSRIGHTS MODIFY;");
			execute("ADD ACE FOR GROUP \"" + siglaNomeProjeto + "-DSV\" GROUP \"" + siglaNomeProjeto + "-ADM\" ACCESSRIGHTS MODIFY;");
			execute("ADD ACE FOR GROUP \"" + siglaNomeProjeto + "-WEB\" GROUP \"" + siglaNomeProjeto + "-ADM\" ACCESSRIGHTS MODIFY;");
			execute("ADD ACE FOR GROUP \"" + siglaNomeProjeto + "-MBL\" GROUP \"" + siglaNomeProjeto + "-ADM\" ACCESSRIGHTS MODIFY;");
		}

		// LIST PROPERTIES FOR ACL FROM FOLDER "Relatórios" IN FOLDER
		// "Objetos públicos" FOR PROJECT
		// "BPM - Indicadores de Desempenho de Processos";
		// LIST PROPERTIES FOR ACL FROM FOLDER "Reports" IN FOLDER
		// "Public Objects" FOR PROJECT
		// "BPM - Indicadores de Desempenho de Processos";

		String[] pastasASeremAlteradas = new String[] { "Objetos públicos", "Objetos do esquema", "Public Objects", "Schema Objects" };

		HashMap permissoesObjetosPublicos = new HashMap();
		permissoesObjetosPublicos.put("ARQ", "FULLCONTROL");
		permissoesObjetosPublicos.put(siglaNomeProjeto + "-DSV", "FULLCONTROL");
		permissoesObjetosPublicos.put(siglaNomeProjeto + "-WEB", "VIEW");

		HashMap permissoesObjetosSchema = new HashMap();
		permissoesObjetosSchema.put("ARQ", "FULLCONTROL");
		permissoesObjetosSchema.put(siglaNomeProjeto, "VIEW");

		HashMap[] permissoes = new HashMap[] { permissoesObjetosPublicos, permissoesObjetosSchema, permissoesObjetosPublicos, permissoesObjetosSchema };

		for (int i = 0; i < pastasASeremAlteradas.length; i++) {
			// Recebe o resultado da consulta
			ResultSet aclPastaReportsRS = executeCapture("LIST PROPERTIES FOR ACL FROM FOLDER \"" + pastasASeremAlteradas[i] + "\" IN FOLDER \"\\\" FOR PROJECT \"" + nomeProjeto + "\";");

			if (aclPastaReportsRS.getRowCount() > 0) {

				// Anda pelo primeiro elemento do ResultSet aclPastaPrincipalRS
				aclPastaReportsRS.moveFirst();
				while (!aclPastaReportsRS.isEof()) {

					// INÍCIO - Implementação sobre o resultset
					// aclPastaPrincipalRS
					String trusteeName = aclPastaReportsRS.getFieldValueString(TRUSTEE_NAME);
					String pattern = "(.*)(\\(+)(\\w+)(\\)?)";
					trusteeName = trusteeName.replaceAll(pattern, "$3");
					String trusteeType = aclPastaReportsRS.getFieldValueString(TRUSTEE_TYPE).equals("User Group") ? "GROUP" : "USER";

					if (!trusteeName.equals("")) {
						execute("REMOVE ACE FROM FOLDER \"" + pastasASeremAlteradas[i] + "\" IN FOLDER \"\\\" " + trusteeType + " '" + trusteeName + "' FOR PROJECT '" + nomeProjeto + "';");
						printOut("Removendo acesso " + trusteeName + " na pasta '\\" + pastasASeremAlteradas[i] + " projeto: " + nomeProjeto);
					}

					// FIM - Implementação sobre o resultset aclPastaPrincipalRS

					// Da continuidade a iteração com o ResultSet
					// aclPastaPrincipalRS
					aclPastaReportsRS.moveNext();
				}

				for (Iterator iterator = permissoes[i].keySet().iterator(); iterator.hasNext();) {
					String grupo = (String) iterator.next();
					execute("ADD ACE FOR FOLDER \"" + pastasASeremAlteradas[i] + "\" IN FOLDER \"\\\" GROUP \"" + grupo + "\" ACCESSRIGHTS " + permissoes[i].get(grupo) + " CHILDRENACCESSRIGHTS " + permissoes[i].get(grupo) + " FOR PROJECT \"" + nomeProjeto + "\";");

				}
				execute("ALTER ACL FOR FOLDER \"" + pastasASeremAlteradas[i] + "\" IN FOLDER \"\\\" PROPAGATE OVERWRITE RECURSIVELY FOR PROJECT \"" + nomeProjeto + "\";");
			}
		}

	}

	public void listMembersForUserGroupEveryone() {
		// Recebe o resultado da consulta
		ResultSet loginUsuarioRS = executeCapture("LIST MEMBERS FOR USER GROUP \"Everyone\";");
		loginUsuarioRS.moveFirst();
		loginUsuarioRS = (ResultSet) loginUsuarioRS.getFieldValue(MEMBER_RESULTSET);
		loginUsuarioRS.getRowCount();

		// Verifica se existe algum resultado
		if (loginUsuarioRS.getRowCount() > 0) {

			// Anda pelo primeiro elemento do ResultSet loginUsuarioRS
			loginUsuarioRS.moveFirst();
			while (!loginUsuarioRS.isEof()) {

				if (loginUsuarioRS.getFieldValueString(IS_GROUP).equals("false")) {
					// INÍCIO - Implementação sobre o resultset loginUsuarioRS
					printOut(loginUsuarioRS.getFieldValueString(LOGIN));
					// FIM - Implementação sobre o resultset loginUsuarioRS
				}

				// Da continuidade a iteração com o ResultSet
				// loginUsuarioRS
				loginUsuarioRS.moveNext();
			}
		}
	}

	public void excluirProjetoPadraoCopel(String nomeProjeto) {
		// String nomeProjeto = "ING - Gestão de Indicadores";
		// String descricaoProjeto =
		// "Aplicativo BI para análise e acompanhamento dos indicadores estratégicos da Copel";
		String siglaNomeProjeto = nomeProjeto.substring(0, 3);

		// Delete everything
		execute("DELETE PROJECT \"" + nomeProjeto + "\";");
		execute("DELETE DBINSTANCE \"Warehouse - " + nomeProjeto + "\";");
		execute("DELETE DBCONNECTION \"DC - " + nomeProjeto + "\";");
		execute("DELETE DBLOGIN \"Login - " + nomeProjeto + "\";");
		execute("DELETE USER GROUP \"" + siglaNomeProjeto + "\";");
		execute("DELETE USER GROUP \"" + siglaNomeProjeto + "-ADM\";");
		execute("DELETE USER GROUP \"" + siglaNomeProjeto + "-DSV\";");
		execute("DELETE USER GROUP \"" + siglaNomeProjeto + "-MBL\";");
		execute("DELETE USER GROUP \"" + siglaNomeProjeto + "-WEB\";");
	}

	public void navegarDiretoriosProjetos() {

		String dir = "\\\\Public Objects\\Reports";
		String projeto = "AIN - Analise de Interrupcoes";
		
		// Recebe o resultado da consulta
		ResultSet listaDirRS = (ResultSet) executeCapture("LIST ALL FOLDERS IN \"" + dir + "\" FOR PROJECT \"" + projeto + "\";");
		
		// Verifica se existe algum resultado
		if (listaDirRS.getRowCount() > 0) {
		
			// Anda pelo primeiro elemento do ResultSet listaDirRS
			listaDirRS.moveFirst();
			while (!listaDirRS.isEof()) {
		
				// INÍCIO - Implementação sobre o resultset listaDirRS
				String newDir = dir + "\\" + listaDirRS.getFieldValueString(NAME);
				printOut(newDir);
				// FIM - Implementação sobre o resultset listaDirRS
		
				// Da continuidade a iteração com o ResultSet
				// listaDirRS
				listaDirRS.moveNext();
			}
		}
	}

	public void recursivelyListFolders(String navegarRecursivo) {

		String sTopFolder = "\\\\Public Objects\\Reports";
		String sProject = "AIN - Analise de Interrupcoes";

		printOut("Displaying recursively folders for " + sTopFolder);
		// Retrieve the initial folder list under the specified top folder
		ResultSet oResultSet = executeCapture("LIST ALL FOLDERS IN \"" + sTopFolder + "\" FOR PROJECT \"" + sProject + "\";");
		oResultSet.moveFirst();
		// Initialize a Cache for folders.
		List<ResultSet> oResultSetCache = new ArrayList<ResultSet>();
		oResultSetCache.add(oResultSet);

		// Loop until there is no more folder list cached.
		ProcessNextFolderLabel: while (oResultSetCache.size() > 0) {
			oResultSet = (ResultSet) oResultSetCache.get(oResultSetCache.size() - 1);
			// The ResultSet always picks up at its last pointed element
			while (!oResultSet.isEof()) {
				// Retrieve the next Folder in the list
				String sTraversedFolder = oResultSet.getFieldValueString(NAME);
				String path = oResultSet.getFieldValueString(PATH);
				sTraversedFolder = path + sTraversedFolder;
				printOut("traversing " + sTraversedFolder);

				oResultSet.moveNext();

				// Query the subfolders.
				ResultSet oResultSet2 = executeCapture("LIST ALL FOLDERS IN \"" + sTraversedFolder + "\" FOR PROJECT \"" + sProject + "\";");
				if (oResultSet2.getRowCount() > 0) {
					// There are more subfolders. Add it to the list and process
					// it.
					oResultSet2.moveFirst();
					oResultSetCache.add(oResultSet2);
					// The subfolder processing starts in the outer loop
					continue ProcessNextFolderLabel;
				}
			}

			// That was the last element at this level. Remove it from the
			// cache.
			oResultSetCache.remove(oResultSetCache.size() - 1);
		}
		printOut("Done");
	}

	public void printRemoveAllACEFromFolder() {
		String sFolderName = "";
		String sFolderPath = "";
		String sProject = "";
		// list acl for the folder
		ResultSet oResultSet = executeCapture("LIST ALL PROPERTIES FOR ACL FROM FOLDER '" + sFolderName + "' IN FOLDER '" + sFolderPath + "' FOR PROJECT '" + sProject + "';");
		String sUserName = null;
		if (oResultSet.getRowCount() > 0) {
			oResultSet.moveFirst();
			// Go through each ACE and delete them one by one
			while (!oResultSet.isEof()) {
				sUserName = (String) oResultSet.getFieldValue(DisplayPropertyEnum.TRUSTEE_NAME);
				if (sUserName != null) {
					if (sUserName.indexOf("(") > 0 && sUserName.indexOf(")") > 0) {
						// sometimes, the username is enclosed by (), need to
						// remove ()
						sUserName = sUserName.substring(sUserName.indexOf("(") + 1, sUserName.indexOf(")")).trim();
						printOut("REMOVE ACE FROM FOLDER '" + sFolderName + "' IN FOLDER '" + sFolderPath + "' USER '" + sUserName + "' FOR PROJECT '" + sProject + "';");
					} else {
						printOut("REMOVE ACE FROM FOLDER '" + sFolderName + "' IN FOLDER '" + sFolderPath + "' GROUP '" + sUserName + "' FOR PROJECT '" + sProject + "';");
					}
				}
				oResultSet.moveNext();
			}
			printOut("All ACE for folder '" + sFolderName + "' under '" + sFolderPath + "' folder from '" + sProject + "' project has been removed successfully.");
		} else {
			printOut("No ACE exists for the specified folder.");
		}
	}
	
	public void addAceOnFolder(){

		int numOfDays = 1;
		String folderName = "";
		String projectName = "";
		String userName = "";
		
		printOut("starting execution...");
		//to get creation time that is numOfDays before today
		Calendar oTime = Calendar.getInstance();
		
		oTime.set(Calendar.DAY_OF_MONTH, oTime.get(Calendar.DAY_OF_MONTH) - numOfDays);

		
		// Retrieve the initial folder list under the specified top folder
		String sQuery = "LIST ALL FOLDERS IN \"" + folderName + "\" FOR PROJECT \"" + projectName + "\";";
		ResultSet oFolders = executeCapture(sQuery);

		// Initialize a Cache for folders.
		Vector<ResultSet> oResultSetCache = new Vector<ResultSet>();
		oResultSetCache.add(oFolders);

		// Get all reports under the initial folder
		sQuery = "LIST ALL REPORTS IN FOLDER \"" + folderName + "\" FOR PROJECT '" + projectName + "';";
		ResultSet oReports = executeCapture(sQuery);
		oReports.moveFirst();
		while(!oReports.isEof()){    
			String sReportName = oReports.getFieldValueString(DisplayPropertyEnum.NAME); 
			sQuery = "LIST ALL PROPERTIES FOR REPORT \"" + sReportName + "\" IN FOLDER \"" + folderName + "\" FOR PROJECT \"" + projectName + "\";";
			ResultSet reportProperties = executeCapture(sQuery);
			reportProperties.moveFirst();
			Date reportDate = (Date) reportProperties.getFieldValue(DisplayPropertyEnum.CREATION_TIME);
			if(reportDate != null && reportDate.after(oTime.getTime())){
				/** doing ACE work here for reports in the initial folder **/
				sQuery = "ADD ACE FOR REPORT \"" + sReportName + "\" IN FOLDER \"" + folderName + "\" USER \"" + userName + "\" ACCESSRIGHTS FULLCONTROL FOR PROJECT \"" + projectName + "\";";
				execute(sQuery);
				printOut("add '" + userName + "' ace successfully for report '" + sReportName + "' in '" + folderName + "'");
				/** ending of ACE work for reports in the initial folder **/ 
			}
			oReports.moveNext();
		}

		// process all subfolders and reports in the subfolders recursively
		while (oResultSetCache.size() > 0){
			ResultSet oChildFolders = (ResultSet)oResultSetCache.lastElement();
			oResultSetCache.remove(oResultSetCache.size() - 1);
			oChildFolders.moveFirst();
			while (!oChildFolders.isEof()){
				// Retrieve the next Folder in the list  
				String sPath = oChildFolders.getFieldValue(DisplayPropertyEnum.PATH).toString();
				String sFolderName = oChildFolders.getFieldValue(DisplayPropertyEnum.NAME).toString();
				folderName = sPath + sFolderName;
				oChildFolders.moveNext();

				// Get all reports under this folder
				sQuery = "LIST ALL REPORTS IN FOLDER \"" + folderName + "\" FOR PROJECT \"" + projectName + "\";";
				oReports = executeCapture(sQuery);
				oReports.moveFirst();  
				while(!oReports.isEof()){    
					String sReportName = oReports.getFieldValueString(DisplayPropertyEnum.NAME);
					sQuery = "LIST ALL PROPERTIES FOR REPORT \"" + sReportName
						+ "\" IN FOLDER \"" + folderName + "\" FOR PROJECT \"" + projectName + "\";";
					ResultSet reportProperties = executeCapture(sQuery);
					reportProperties.moveFirst();

					//process the reports that are created after the certain time
					Date reportCreateTime = (Date) reportProperties.getFieldValue(DisplayPropertyEnum.CREATION_TIME);
					if(reportCreateTime != null && reportCreateTime.after(oTime.getTime())){
						/** do ACE action here for reports in the current folder **/
						sQuery = "ADD ACE FOR REPORT \"" + sReportName + "\" IN FOLDER \"" + folderName + "\" USER \"" + userName
							+ "\" ACCESSRIGHTS FULLCONTROL FOR PROJECT \"" + projectName + "\";";
						execute(sQuery);
						printOut("add '" + userName + "' ace successfully for report '" + sReportName + "' in '" + folderName + "'");
						/** ending of ACE action for reports in the current folder **/
					}

					oReports.moveNext();
				}

				sQuery = "LIST ALL PROPERTIES FOR FOLDER \"" + sFolderName + "\" IN \"" + sPath + "\" FOR PROJECT \"" + projectName + "\";";
				ResultSet folderProperties = executeCapture(sQuery);
				folderProperties.moveFirst();

				//process the folders that are created after the certain time
				Date folderCreateTime = (Date) folderProperties.getFieldValue(DisplayPropertyEnum.CREATION_TIME);
				if(folderCreateTime != null && folderCreateTime.after(oTime.getTime())){
					/** do ACE action here for the current folder **/ 
					sQuery = "ADD ACE FOR FOLDER \"" + sFolderName + "\" IN FOLDER \"" + sPath + "\" USER \"" + userName
						+ "\" ACCESSRIGHTS FULLCONTROL CHILDRENACCESSRIGHTS MODIFY FOR PROJECT \"" + projectName + "\";";
					execute(sQuery);
					printOut("add '" + userName + "' ace successfully for folder '" + folderName + "'");
					/*** ending of ace action on the current folder **/
				}

				// get the subfolders in the current folder
				sQuery = "LIST ALL FOLDERS IN \"" + folderName + "\" FOR PROJECT \"" + projectName + "\";";
				ResultSet oChildSubFolders = executeCapture(sQuery);

				if (oChildSubFolders.getRowCount() > 0){  
					oResultSetCache.add(oChildSubFolders);
				}
			}    
		}

		printOut("Done.");
	}

}
