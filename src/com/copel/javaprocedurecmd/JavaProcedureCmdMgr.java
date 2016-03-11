package com.copel.javaprocedurecmd;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.Locale.Category;

import com.copel.cmdutil.ResultSet;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class JavaProcedureCmdMgr extends CmdMgrBase {

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
			execute("ADD ACE FOR GROUP \"" + siglaNomeProjeto + "-WEB\" GROUP \"DESENV\" ACCESSRIGHTS MODIFY;");
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

	public void listAllProjects(){
		// Recebe o resultado da consulta
		ResultSet projectsRS = (ResultSet) executeCapture("LIST ALL PROJECTS;");

		// Verifica se existe algum resultado
		if (projectsRS.getRowCount() > 0) {

			// Anda pelo primeiro elemento do ResultSet projectsRS
			projectsRS.moveFirst();
			while (!projectsRS.isEof()) {

				// INÍCIO - Implementação sobre o resultset projectsRS
				printOut(projectsRS.getFieldValueString(NAME));
				// FIM - Implementação sobre o resultset projectsRS

				// Da continuidade a iteração com o ResultSet
				// projectsRS
				projectsRS.moveNext();
			}
		}
	}

	public void recursivelyNavigateGroups(){

		String topGroup = "PROJ";

		// Retrieve the initial folder list under the specified top folder
		ResultSet oResultSet = executeCapture("LIST MEMBERS FOR USER GROUP \"" + topGroup + "\";");
		oResultSet.moveFirst();
		oResultSet = (ResultSet) oResultSet.getFieldValue(MEMBER_RESULTSET);
		oResultSet.moveFirst();

		// Initialize a Cache for folders.
		List<ResultSet> oResultSetCache = new ArrayList<ResultSet>();
		oResultSetCache.add(oResultSet);

		// Loop until there is no more folder list cached.
		ProcessNextFolderLabel: while (oResultSetCache.size() > 0){
			oResultSet = (ResultSet)oResultSetCache.get(oResultSetCache.size()-1);
			// The ResultSet always picks up at its last pointed element
			while (!oResultSet.isEof()){

				if(!(Boolean)oResultSet.getFieldValue(IS_GROUP)){
					oResultSet.moveNext();
					continue ;
				}

				// Retrieve the next Folder in the list
				String sTraversedGroup =  oResultSet.getFieldValueString(LOGIN);

				// Recebe o resultado da consulta
				ResultSet idRS = (ResultSet) executeCapture("LIST PROPERTIES FOR USER GROUP \"" + sTraversedGroup + "\";");

				// Verifica se existe algum resultado
				if (idRS.getRowCount() > 0) {

					// Anda pelo primeiro elemento do ResultSet idRS
					idRS.moveFirst();
					while (!idRS.isEof()) {

						// INÍCIO - Implementação sobre o resultset idRS

						printOut(sTraversedGroup + "\t\t" + idRS.getFieldValueString(ID));
						// FIM - Implementação sobre o resultset idRS

						// Da continuidade a iteração com o ResultSet
						// idRS
						idRS.moveNext();
					}
				}

				oResultSet.moveNext();

				// Query the subfolders.
				ResultSet oResultSet2 = executeCapture("LIST MEMBERS FOR USER GROUP \"" + sTraversedGroup + "\";");
				oResultSet2.moveFirst();
				oResultSet2 = (ResultSet) oResultSet2.getFieldValue(MEMBER_RESULTSET);
				if (oResultSet2.getRowCount() > 0){
					// There are more subfolders. Add it to the list and process it.
					oResultSet2.moveFirst();
					oResultSetCache.add(oResultSet2);
					// The subfolder processing starts in the outer loop
					continue ProcessNextFolderLabel;
				}
			}

			// That was the last element at this level. Remove it from the cache.
			oResultSetCache.remove(oResultSetCache.size() - 1);
		}
		printOut("Done");
	}


	public void printGroupAndId(){
		LinkedList ll  = new LinkedList();
		ll.add("AIN");
		ll.add("AIN-ADM");
		ll.add("AIN-DSV");
		ll.add("AIN-MBL");
		ll.add("AIN-WEB");
		ll.add("ANC");
		ll.add("ANC-ADM");
		ll.add("ANC-DSV");
		ll.add("ANC-MBL");
		ll.add("ANC-WEB");
		ll.add("ANC-WEB-DEP-SUB");
		ll.add("ANC-WEB-DIR-SUB");
		ll.add("ANC-WEB-GENERICO");
		ll.add("ANC-WEB-ORG3");
		ll.add("ANC-WEB-ORG4");
		ll.add("ANC-WEB-ORG4-PRE");
		ll.add("ANC-WEB-ORG5");
		ll.add("ANC-WEB-ORG5-PRE");
		ll.add("ANC-WEB-ORG6");
		ll.add("ANC-WEB-ORG7");
		ll.add("ANC-WEB-POE");
		ll.add("ANC-WEB-RH-CTE");
		ll.add("ANC-WEB-RH-DIS");
		ll.add("ANC-WEB-RH-GET");
		ll.add("ANC-WEB-SUP-SUB");
		ll.add("ANC-WEB-SUP-TODOS");
		ll.add("ANC-WEB-TOTAL");
		ll.add("AOV");
		ll.add("AOV-ADM");
		ll.add("AOV-DSV");
		ll.add("AOV-MBL");
		ll.add("AOV-WEB");
		ll.add("AOV-WEB-DEP");
		ll.add("AOV-WEB-DIR");
		ll.add("AOV-WEB-DIR-SUB");
		ll.add("AOV-WEB-PRE");
		ll.add("AOV-WEB-SUP");
		ll.add("AOV-WEB-SUP-SUB");
		ll.add("AOV-WEB-TOTAL");
		ll.add("APT");
		ll.add("APT-ADM");
		ll.add("APT-DSV");
		ll.add("APT-MBL");
		ll.add("APT-WEB");
		ll.add("ARH");
		ll.add("ARH-ADM");
		ll.add("ARH-DSV");
		ll.add("ARH-MBL");
		ll.add("ARH-WEB");
		ll.add("ASE");
		ll.add("ASE-ADM");
		ll.add("ASE-DSV");
		ll.add("ASE-MBL");
		ll.add("ASE-WEB");
		ll.add("DESENV");
		ll.add("GAI-DGC");
		ll.add("GAI-Diretorias");
		ll.add("GCI");
		ll.add("GCI-ADM");
		ll.add("GCI-DSV");
		ll.add("GCI-MBL");
		ll.add("GCI-WEB");
		ll.add("GIE");
		ll.add("GIE-ADM");
		ll.add("GIE-DSV");
		ll.add("GIE-MBL");
		ll.add("GIE-WEB");
		ll.add("GIR");
		ll.add("GIR-ADM");
		ll.add("GIR-DSV");
		ll.add("GIR-MBL");
		ll.add("GIR-WEB");
		ll.add("GRC");
		ll.add("GRC-ADM");
		ll.add("GRC-DSV");
		ll.add("GRC-MBL");
		ll.add("GRC-WEB");
		ll.add("GRC-WEB-REGIONAIS");
		ll.add("GRC-WEB-TOTAL");
		ll.add("IDE");
		ll.add("IDE-ADM");
		ll.add("IDE-DSV");
		ll.add("IDE-MBL");
		ll.add("IDE-WEB");
		ll.add("IGE");
		ll.add("IGE-ADM");
		ll.add("IGE-DSV");
		ll.add("IGE-MBL");
		ll.add("IGE-WEB");
		ll.add("IIC");
		ll.add("IIC-ADM");
		ll.add("IIC-DSV");
		ll.add("IIC-MBL");
		ll.add("IIC-WEB");
		ll.add("IIE");
		ll.add("IIE-ADM");
		ll.add("IIE-DSV");
		ll.add("IIE-MBL");
		ll.add("IIE-WEB");
		ll.add("ING");
		ll.add("ING-ADM");
		ll.add("ING-DSV");
		ll.add("ING-DSV-DIS");
		ll.add("ING-MBL");
		ll.add("ING-WEB");
		ll.add("ING-WEB-DIS");
		ll.add("IQO");
		ll.add("IQO-ADM");
		ll.add("IQO-DSV");
		ll.add("IQO-MBL");
		ll.add("IQO-WEB");
		ll.add("ITE");
		ll.add("ITE-ADM");
		ll.add("ITE-DSV");
		ll.add("ITE-MBL");
		ll.add("ITE-WEB");
		ll.add("ITE-WEB-SUP");
		ll.add("SOP");
		ll.add("SOP-ADM");
		ll.add("SOP-DSV");
		ll.add("SOP-MBL");
		ll.add("SOP-WEB");
		ll.add("TRT");
		ll.add("TRT-ADM");
		ll.add("TRT-DSV");
		ll.add("TRT-MBL");
		ll.add("TRT-WEB");

		for (int i = 0; i < ll.size(); i++) {

			String groupName = (String) ll.get(i);

			// Recebe o resultado da consulta
			ResultSet idRS = (ResultSet) executeCapture("LIST PROPERTIES FOR USER GROUP \"" + groupName + "\";");

			// Verifica se existe algum resultado
			if (idRS.getRowCount() > 0) {

				// Anda pelo primeiro elemento do ResultSet idRS
				idRS.moveFirst();
				while (!idRS.isEof()) {

					// INÍCIO - Implementação sobre o resultset idRS

					printOut(groupName + "\t\t" + idRS.getFieldValueString(ID));
					// FIM - Implementação sobre o resultset idRS

					// Da continuidade a iteração com o ResultSet
					// idRS
					idRS.moveNext();
				}
			}
		}
	}

	public void listAllTokensSecurityRole(){
		// Recebe o resultado da consulta
		ResultSet securityRoleRS = (ResultSet) executeCapture("LIST SECURITY ROLES;");

		// Verifica se existe algum resultado
		if (securityRoleRS.getRowCount() > 0) {

			// Anda pelo primeiro elemento do ResultSet securityRoleRS
			securityRoleRS.moveFirst();
			while (!securityRoleRS.isEof()) {

				// INÍCIO - Implementação sobre o resultset securityRoleRS
				String secRole = securityRoleRS.getFieldValueString(NAME);

				printOut("");
				printOut(secRole);

				// Recebe o resultado da consulta
				ResultSet privilegesRS = executeCapture("LIST ALL PRIVILEGES FOR SECURITY ROLE \"" + secRole + "\";");
				privilegesRS.moveFirst();

				// Verifica se existe algum resultado
				while (!privilegesRS.isEof()) {

					ResultSet tokenRS = (ResultSet) privilegesRS.getFieldValue(1);
					tokenRS.getRowCount();

					// Anda pelo primeiro elemento do ResultSet consultaVarRS
					tokenRS.moveFirst();
					while (!tokenRS.isEof()) {

						// INÍCIO - Implementação sobre o resultset consultaVarRS
						printOut(tokenRS.getFieldValueString(1));
						// FIM - Implementação sobre o resultset consultaVarRS

						// Da continuidade a iteração com o ResultSet
						// consultaVarRS
						tokenRS.moveNext();
					}
					privilegesRS.moveNext();
				}

				// FIM - Implementação sobre o resultset securityRoleRS

				// Da continuidade a iteração com o ResultSet
				// securityRoleRS
				securityRoleRS.moveNext();
			}
		}
	}

	public void listMembersSecurityRoleByProject(){
		// Recebe o resultado da consulta
		ResultSet projectRS = (ResultSet) executeCapture("LIST ALL PROJECTS;");

		// Verifica se existe algum resultado
		if (projectRS.getRowCount() > 0) {

			// Anda pelo primeiro elemento do ResultSet projectRS
			projectRS.moveFirst();
			while (!projectRS.isEof()) {

				// INÍCIO - Implementação sobre o resultset projectRS
				String projectName = projectRS.getFieldValueString(NAME);

				// Recebe o resultado da consulta
				ResultSet securityRoleRS = (ResultSet) executeCapture("LIST SECURITY ROLES;");

				// Verifica se existe algum resultado
				if (securityRoleRS.getRowCount() > 0) {

					// Anda pelo primeiro elemento do ResultSet securityRoleRS
					securityRoleRS.moveFirst();
					while (!securityRoleRS.isEof()) {

						// INÍCIO - Implementação sobre o resultset securityRoleRS
						String secRoleName = securityRoleRS.getFieldValueString(NAME);

						// Recebe o resultado da consulta
						ResultSet membersSecRoleByProjectRS = (ResultSet) executeCapture("LIST ALL MEMBERS FOR SECURITY ROLE \"" + secRoleName + "\" IN PROJECT \"" + projectName + "\";");
						membersSecRoleByProjectRS.moveFirst();
						membersSecRoleByProjectRS = (ResultSet) membersSecRoleByProjectRS.getFieldValue(MEMBER_RESULTSET);
						membersSecRoleByProjectRS.moveFirst();

						// Verifica se existe algum resultado
						if (membersSecRoleByProjectRS.getRowCount() > 0) {

							// Anda pelo primeiro elemento do ResultSet membersSecRoleByProjectRS
							membersSecRoleByProjectRS.moveFirst();
							while (!membersSecRoleByProjectRS.isEof()) {

								// INÍCIO - Implementação sobre o resultset membersSecRoleByProjectRS
								String membersSecRoleByProject = membersSecRoleByProjectRS.getFieldValueString(0);
								printOut(projectName + ";" + secRoleName + ";" + membersSecRoleByProject);
								// FIM - Implementação sobre o resultset membersSecRoleByProjectRS

								// Da continuidade a iteração com o ResultSet
								// membersSecRoleByProjectRS
								membersSecRoleByProjectRS.moveNext();
							}
						}

						// FIM - Implementação sobre o resultset securityRoleRS

						// Da continuidade a iteração com o ResultSet
						// securityRoleRS
						securityRoleRS.moveNext();
					}
				}

				// FIM - Implementação sobre o resultset projectRS

				// Da continuidade a iteração com o ResultSet
				// projectRS
				projectRS.moveNext();
			}
		}
	}

	public void tempListMembersSecurityRoleByProject(){

		// Recebe o resultado da consulta
		ResultSet membersSecRoleByProjectRS = (ResultSet) executeCapture("LIST ALL MEMBERS FOR SECURITY ROLE \"Web\" IN PROJECT \"AIN - Analise de Interrupcoes\";");
		membersSecRoleByProjectRS.moveFirst();
		membersSecRoleByProjectRS = (ResultSet) membersSecRoleByProjectRS.getFieldValue(MEMBER_RESULTSET);
		membersSecRoleByProjectRS.moveFirst();

		// Verifica se existe algum resultado
		if (membersSecRoleByProjectRS.getRowCount() > 0) {

			// Anda pelo primeiro elemento do ResultSet membersSecRoleByProjectRS
			membersSecRoleByProjectRS.moveFirst();
			while (!membersSecRoleByProjectRS.isEof()) {

				// INÍCIO - Implementação sobre o resultset membersSecRoleByProjectRS
				String membersSecRoleByProject = membersSecRoleByProjectRS.getFieldValueString(NAME);
				printOut(membersSecRoleByProject);
				// FIM - Implementação sobre o resultset membersSecRoleByProjectRS

				// Da continuidade a iteração com o ResultSet
				// membersSecRoleByProjectRS
				membersSecRoleByProjectRS.moveNext();
			}
		}

	}

	public void addEMWarehouseToProjects(){
		// Recebe o resultado da consulta
		ResultSet listProjectRS = executeCapture("LIST PROJECTS;");

		// Verifica se existe algum resultado
		if (listProjectRS.getRowCount() > 0) {

			// Anda pelo primeiro elemento do ResultSet listProjectRS
			listProjectRS.moveFirst();
			while (!listProjectRS.isEof()) {

				// INÍCIO - Implementação sobre o resultset listProjectRS
				execute("ALTER STATISTICS DBINSTANCE \"Warehouse - Enterprise Manager\" " +
						"BASICSTATS ENABLED " +
						"DETAILEDREPJOBS TRUE " +
						"DETAILEDDOCJOBS TRUE " +
						"JOBSQL TRUE " +
						"COLUMNSTABLES TRUE " +
						"PROMPTANSWERS TRUE " +
						"SUBSCRIPTIONDELIVERIES TRUE " +
						"IN PROJECT \"" + listProjectRS.getFieldValueString(NAME) + "\";");
				// FIM - Implementação sobre o resultset listProjectRS

				// Da continuidade a iteração com o ResultSet
				// listProjectRS
				listProjectRS.moveNext();
			}
		}
	}

	public void criarLinkPorProjeto(){
		String serverURL = "apldwhprd";
		String serverNameUpperCase = "APLDWHPRD";
		String urlLink = "http://%s/MicroStrategyLDAP/servlet/mstrServerAdmin?evt=1001&src=mstrServerAdmin.groups.ugb.1001&objectID=%s&Server=%s";

		// Recebe o resultado da consulta
		ResultSet projetosRS = executeCapture("LIST ALL PROJECTS;");

		// Verifica se existe algum resultado
		if (projetosRS.getRowCount() > 0) {

			// Anda pelo primeiro elemento do ResultSet projetosRS
			projetosRS.moveFirst();
			while (!projetosRS.isEof()) {

				// INÍCIO - Implementação sobre o resultset projetosRS

				String nomeProjeto = projetosRS.getFieldValueString(NAME);
				//nomeProjeto = nomeProjeto.substring(17);
				String siglaNomeProjeto = nomeProjeto.substring(0, 3);

				// Recebe o resultado da consulta
				ResultSet groupPropertiesRS = executeCapture("LIST ALL PROPERTIES FOR USER GROUP \"" + siglaNomeProjeto + "\";");

				// Verifica se existe algum resultado
				if (groupPropertiesRS.getRowCount() > 0) {

					// Anda pelo primeiro elemento do ResultSet groupPropertiesRS
					groupPropertiesRS.moveFirst();
					if (!groupPropertiesRS.isEof()) {

						// INÍCIO - Implementação sobre o resultset groupPropertiesRS
						printOut("<tr>");
						printOut("   <td>" + nomeProjeto + "</td>");
						printOut("   <td><a href=\"" + String.format(urlLink, serverURL, groupPropertiesRS.getFieldValueString(ID), serverNameUpperCase) + "\">Iniciar</a></td>");
						printOut("</tr>");
						// FIM - Implementação sobre o resultset groupPropertiesRS

						// Da continuidade a iteração com o ResultSet
						// groupPropertiesRS
						groupPropertiesRS.moveNext();
					}
				}

				// FIM - Implementação sobre o resultset projetosRS

				// Da continuidade a iteração com o ResultSet
				// projetosRS
				projetosRS.moveNext();
			}
		}

	}

	public void criarGrupoPadraoCopel(){
		String nomeProjeto = "SSB - Self Service BI";
		if ((nomeProjeto.charAt(3) + "").equals(" ")) {
			String siglaNomeProjeto = nomeProjeto.substring(0, 3);

			// Caso o grupo não exista
			execute("CREATE USER GROUP \"" + siglaNomeProjeto + "\" DESCRIPTION \"Agrupa tipos diferentes de permissões de usuários para o projeto " + nomeProjeto + "\"  IN GROUP \"PROJ\";");

			execute("CREATE USER GROUP \"" + siglaNomeProjeto + "-ADM\" DESCRIPTION \"Agupa usuários principais, com permissão de adicionar usuários em grupos, para o projeto " + nomeProjeto + "\" IN GROUP \"" + siglaNomeProjeto + "\";");
			execute("ALTER USER GROUP \"" + siglaNomeProjeto + "-ADM\" GROUP \"USERADM\";");

			execute("CREATE USER GROUP \"" + siglaNomeProjeto + "-DSV\" DESCRIPTION \"Agupa desenvolvedores para o projeto " + nomeProjeto + "\" IN GROUP \"" + siglaNomeProjeto + "\";");
			execute("GRANT SECURITY ROLE \"_Desenvolvedor\" TO GROUP \"" + siglaNomeProjeto + "-DSV\" FOR PROJECT \"" + nomeProjeto + "\";");

			execute("CREATE USER GROUP \"" + siglaNomeProjeto + "-MBL\" DESCRIPTION \"Agupa  usuários finais com permissão mobile para o projeto " + nomeProjeto + "\" IN GROUP \"" + siglaNomeProjeto + "\";");
			execute("GRANT SECURITY ROLE \"_Mobile\" TO GROUP \"" + siglaNomeProjeto + "-MBL\" FOR PROJECT \"" + nomeProjeto + "\";");

			execute("CREATE USER GROUP \"" + siglaNomeProjeto + "-WEB\" DESCRIPTION \"Agupa  usuários finais com permissão web para o projeto " + nomeProjeto + "\" IN GROUP \"" + siglaNomeProjeto + "\";");
			execute("GRANT SECURITY ROLE \"_Web\" TO GROUP \"" + siglaNomeProjeto + "-WEB\" FOR PROJECT \"" + nomeProjeto + "\";");

			execute("CREATE USER GROUP \"" + siglaNomeProjeto + "-SSBI\" DESCRIPTION \"Agupa  usuários finais com permissão Self Service BI para o projeto " + nomeProjeto + "\" IN GROUP \"" + siglaNomeProjeto + "\";");
			execute("GRANT SECURITY ROLE \"_SelfServiceBi\" TO GROUP \"" + siglaNomeProjeto + "-WEB\" FOR PROJECT \"" + nomeProjeto + "\";");

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
					String groupName = aclGroupRS.getFieldValueString(TRUSTEE_NAME);
					execute("REMOVE ACE FROM GROUP \"" + siglaNomeProjeto + "\" GROUP \"" + groupName + "\";");
					execute("REMOVE ACE FROM GROUP \"" + siglaNomeProjeto + "-ADM\" GROUP \"" + groupName + "\";");
					execute("REMOVE ACE FROM GROUP \"" + siglaNomeProjeto + "-DSV\" GROUP \"" + groupName + "\";");
					execute("REMOVE ACE FROM GROUP \"" + siglaNomeProjeto + "-MBL\" GROUP \"" + groupName + "\";");
					execute("REMOVE ACE FROM GROUP \"" + siglaNomeProjeto + "-WEB\" GROUP \"" + groupName + "\";");
					execute("REMOVE ACE FROM GROUP \"" + siglaNomeProjeto + "-SSBI\" GROUP \"" + groupName + "\";");
					// FIM - Implementação sobre o resultset
					// aclGroupRS

					// Da continuidade a iteração com o ResultSet
					// aclGroupRS
					aclGroupRS.moveNext();
				}

				execute("REMOVE ACE FROM GROUP \"" + siglaNomeProjeto + "\" GROUP \"Administrator\";");
				execute("REMOVE ACE FROM GROUP \"" + siglaNomeProjeto + "-ADM\" GROUP \"Administrator\";");
				execute("REMOVE ACE FROM GROUP \"" + siglaNomeProjeto + "-DSV\" GROUP \"Administrator\";");
				execute("REMOVE ACE FROM GROUP \"" + siglaNomeProjeto + "-MBL\" GROUP \"Administrator\";");
				execute("REMOVE ACE FROM GROUP \"" + siglaNomeProjeto + "-WEB\" GROUP \"Administrator\";");
				execute("REMOVE ACE FROM GROUP \"" + siglaNomeProjeto + "-SSBI\" GROUP \"Administrator\";");
				execute("REMOVE ACE FROM GROUP \"" + siglaNomeProjeto + "\" GROUP \"System Monitors\";");
				execute("REMOVE ACE FROM GROUP \"" + siglaNomeProjeto + "-ADM\" GROUP \"System Monitors\";");
				execute("REMOVE ACE FROM GROUP \"" + siglaNomeProjeto + "-DSV\" GROUP \"System Monitors\";");
				execute("REMOVE ACE FROM GROUP \"" + siglaNomeProjeto + "-MBL\" GROUP \"System Monitors\";");
				execute("REMOVE ACE FROM GROUP \"" + siglaNomeProjeto + "-WEB\" GROUP \"System Monitors\";");
				execute("REMOVE ACE FROM GROUP \"" + siglaNomeProjeto + "-SSBI\" GROUP \"System Monitors\";");

				execute("ADD ACE FOR GROUP \"" + siglaNomeProjeto + "\" GROUP \"" + siglaNomeProjeto + "-ADM\" ACCESSRIGHTS MODIFY;");
				execute("ADD ACE FOR GROUP \"" + siglaNomeProjeto + "-WEB\" GROUP \"" + siglaNomeProjeto + "-ADM\" ACCESSRIGHTS MODIFY;");
				execute("ADD ACE FOR GROUP \"" + siglaNomeProjeto + "-MBL\" GROUP \"" + siglaNomeProjeto + "-ADM\" ACCESSRIGHTS MODIFY;");
				execute("ADD ACE FOR GROUP \"" + siglaNomeProjeto + "-SSBI\" GROUP \"" + siglaNomeProjeto + "-ADM\" ACCESSRIGHTS MODIFY;");

				execute("ADD ACE FOR GROUP \"" + siglaNomeProjeto + "\" GROUP \"DESENV\" ACCESSRIGHTS VIEW;");
				execute("ADD ACE FOR GROUP \"" + siglaNomeProjeto + "-WEB\" GROUP \"DESENV\" ACCESSRIGHTS VIEW;");
				execute("ADD ACE FOR GROUP \"" + siglaNomeProjeto + "-MBL\" GROUP \"DESENV\" ACCESSRIGHTS VIEW;");
				execute("ADD ACE FOR GROUP \"" + siglaNomeProjeto + "-SSBI\" GROUP \"DESENV\" ACCESSRIGHTS VIEW;");
				execute("ADD ACE FOR GROUP \"" + siglaNomeProjeto + "-DSV\" GROUP \"DESENV\" ACCESSRIGHTS VIEW;");
			}
		}

		// FIM - Implementação sobre o resultset projetosRS

		// Da continuidade a iteração com o ResultSet
		// projetosRS
	}

	public void listaPrivilegiosSecurityRole(){
		String[] arrSecurityRoleName = new String[]{"_arquitetoBW", "_arquitetoOracle", "_desenvolvedor", "_producao", "_usuarioMobile", "_usuarioSSBI", "_usuarioWeb"};
		StringBuffer[] arrSbTokenList = new StringBuffer[arrSecurityRoleName.length];

		for (int i = 0; i < arrSecurityRoleName.length; i++) {

			arrSbTokenList[i] = new StringBuffer();

			// Recebe o resultado da consulta
			ResultSet categoriaSecurityRoleRS = (ResultSet) executeCapture("LIST PRIVILEGES FOR SECURITY ROLE \"" + arrSecurityRoleName[i] + "\";");

			// Verifica se existe algum resultado
			if (categoriaSecurityRoleRS.getRowCount() > 0) {

				// Anda pelo primeiro elemento do ResultSet categoriaSecurityRoleRS
				categoriaSecurityRoleRS.moveFirst();
				while (!categoriaSecurityRoleRS.isEof()) {

					// Recebe o resultado da consulta
					ResultSet privilegioCategoriaSecurityRoleRS = (ResultSet) categoriaSecurityRoleRS.getFieldValue(1);
					privilegioCategoriaSecurityRoleRS.getRowCount();

					// Verifica se existe algum resultado
					if (privilegioCategoriaSecurityRoleRS.getRowCount() > 0) {

						// Anda pelo primeiro elemento do ResultSet privilegioCategoriaSecurityRoleRS
						privilegioCategoriaSecurityRoleRS.moveFirst();
						while (!privilegioCategoriaSecurityRoleRS.isEof()) {

							// INÍCIO - Implementação sobre o resultset privilegioCategoriaSecurityRoleRS
							arrSbTokenList[i].append(";");
							arrSbTokenList[i].append(privilegioCategoriaSecurityRoleRS.getFieldValueString(1));
							arrSbTokenList[i].append(";");

							// FIM - Implementação sobre o resultset privilegioCategoriaSecurityRoleRS

							// Da continuidade a iteração com o ResultSet
							// privilegioCategoriaSecurityRoleRS
							privilegioCategoriaSecurityRoleRS.moveNext();
						}
					}

					// FIM - Implementação sobre o resultset categoriaSecurityRoleRS

					// Da continuidade a iteração com o ResultSet
					// categoriaSecurityRoleRS
					categoriaSecurityRoleRS.moveNext();
				}
			}
		}		

		String[] defaultTokens = new String[]{"WEBCHANGEUSEROPTIONS", "WEBCHANGEVIEWMODE", "WEBCONFIGURETOOLBARS", "WEBNORMALDRILLING", "WEBEXPORT", "WEBOBJECTSEARCH", "WEBPRINTMODE", "WEBREEXECUTEREPORTAGAINSTWH", "WEBSIMULTANEOUSEXEC", "WEBSORT", "WEBSCHEDULEREPORT", "WEBSWITCHPAGEBY", "WEBUSELOCKEDHEADERS", "WEBUSER", "WEBIMPORTDATA", "WEBIMPORTDATABASE", "WEBMODIFYGRIDLEVELINDOC", "WEBCREATEDERIVEDMETRICS", "WEBDEFINEDERIVEDELEMENTS", "WEBNUMBERFORMATTING", "WEBEXECUTECUBEREPORT", "WEBUSEREPORTOBJECTSWINDOW", "WEBUSEVIEWFILTEREDITOR", "WEBCREATEANALYSIS", "WEBVISUALINSIGHT", "WEBSAVEANALYSIS", "WEBCREATEALERT", "WEBEXECUTEBULKEXPORT", "WEBADDTOHISTORYLIST", "WEBADVANCEDDRILLING", "WEBALIASOBJECTS", "WEBCHOOSEATTRFORMDISPLAY", "WEBCREATENEWREPORT", "WEBDRILLONMETRICS", "WEBEDITNOTES", "WEBEXECDATAMARTREPORTS", "WEBFILTERSELECTIONS", "WEBMANAGEOBJECTS", "WEBMODIFYSUBTOTALS", "WEBPIVOTREPORT", "WEBREPORTDETAILS", "WEBREPORTSQL", "WEBSAVEREPORT", "WEBSAVESHAREDREPORT", "WEBSIMPLEGRAPHFORMATTING", "WEBUSEOBJECTSHARINGEDITOR", "WEBUSEBASICTHRESHOLDEDITOR", "WEBDEFINEVIEWREPORT", "WEBSAVEDERIVEDELEMENTS", "WEBCREATEHTMLCONTAINER", "WEBDOCDESIGN", "WEBMANAGEDOCDATASETS", "WEBCONFIGURETRANSACTION", "WEBDEFINEADVANCEDREPORTOPTIONS", "WEBDEFINEOLAPCUBEREP", "WEBEDITREPORTLINKS", "WEBFORMATGRIDANDGRAPH", "WEBMODIFYREPORTLIST", "WEBSAVETEMPLATEFILTER", "WEBSETCOLUMNWIDTHS", "SUBSCRIBEOTHERS", "WEBUSEADVANCEDTHRESHOLDEDITOR", "WEBUSECUSTOMGROUPEDITOR", "WEBUSEDESIGNMODE", "WEBUSEREPORTFILTEREDITOR", "WEBUSEMETRICEDITOR", "WEBUSEPROMPTEDITOR", "DRILLWITHINTELLIGENTCUBE", "USEDYNAMICSOURCING", "USEOLAPSERVICES", "WEBEXECUTEANALYSIS", "WEBEXECUTEDOCUMENT", "EXECUTETRANSACTION", "ADDNOTES", "CREATEAPPOBJECTS", "CREATENEWFOLDER", "CREATESCHEMAOBJECTS", "CREATESHORTCUT", "EDITNOTES", "EXPORTTOEXCEL", "EXPORTTOFLASH", "EXPORTTOHTML", "EXPORTTOPDF", "EXPORTTOTEXT", "SAVEPERSONALPROMPTANSWERS", "SCHEDULEREQUEST", "USESERVERCACHE", "USETRANSLATIONEDITOR", "USETRANSLATIONEDITORBYPASS", "WEBVIEWHISTORYLIST", "VIEWNOTES", "USEOFFICE", "MOBILEEDITANALYSIS", "MOBILEVIEWANALYSIS", "MOBILEVIEWDOCUMENT", "MOBILESAVEANALYSIS", "EMAILSCREENSHOTFROMDEVICE", "MOBILESAVEREPORT", "MOBILEPUBLISH", "PRINTFROMDEVICE", "USEMOBILE", "WEBCREATEDYNAMICADDRESSLIST", "WEBCREATEEMAILADDRESS", "WEBCREATEFILELOCATION", "WEBCREATEPRINTLOCATION", "WEBSUBSCRIPBEDYNAMICADDRESSLIST", "WEBSCHEDULEEMAIL", "WEBSCHEDULEDEXPORTTOFILE", "WEBSCHEDULEDPRINTING", "USEDISTRIBUTIONSERVICES", "SENDLINK", "USEIMMEDIATEDELIVERY", "USESENDPREVIEWNOW", "EXECUTEMULTISOURCEREPORT", "IMPORTTABLEFROMMULTIPLESOURCES", "CREATEDERIVEDMETRICS", "DEFINEDERIVEDELEMENTS", "USEREPORTOBJECTSWINDOW", "USEVIEWFILTEREDITOR", "EXECUTEDOCUMENT", "ALIASOBJECTS", "CHANGEUSERPREFERENCES", "CONFIGURETOOLBARS", "DRILLANDLINK", "MODIFYSUBTOTALS", "MODIFYSORTING", "PIVOTREPORT", "REEXECUTEREPORTAGAINSTWH", "SAVECUSTOMAUTOSTYLE", "SENDTOEMAIL", "SETATTRIBUTEDISPLAY", "USEDATAEXPLORER", "USEDESKTOP", "USEGRIDOPTIONS", "USEHISTORYLIST", "USEREPORTDATAOPTIONS", "USEREPORTEDITOR", "USESEARCHEDITOR", "USETHRESHOLDSEDITOR", "VIEWSQL", "DEFINEVIEWREPORT", "EXECUTECUBEREPORT", "SAVEDERIVEDELEMENTS", "USECUBEREPORTEDITOR", "CREATEHTMLCONTAINER", "USEHTMLDOCUMENTEDITOR", "USEBULKEXPORTEDITOR", "DEFINETRANSACTIONREPORT", "DEFINEOLAPCUBEREPORT", "DEFINEQUERYBUILDERREP", "FORMATGRAPH", "MODIFYREPORTOBJECTLIST", "USECONSOLIDATIONEDITOR", "USECUSTOMGROUPEDITOR", "USEDATAMARTEDITOR", "USEDESIGNMODE", "USEDRILLMAPEDITOR", "USEREPORTFILTEREDITOR", "USEFINDANDREPLACEDIALOG", "USEFORMATTINGEDITOR", "USEFREEFORMSQLEDITOR", "USEHTMLDOCUMENTEDITOR", "EDITREPORTLINKS", "USEMETRICEDITOR", "USEPROJECTDOCUMENTATION", "USEPROMPTEDITOR", "USESQLSTATEMENTSTAB", "USESUBTOTALSEDITOR", "USETEMPLATEEDITOR", "USEVLDBEDITOR", "VIEWETLINFO", "BYPASSSCHEMAACCESSCHECKS", "IMPORTFUNCTION", "IMPORTOLAPCUBE", "USEARCHITECTEDITORS", "USEOBJECTMANAGER", "USEOBJECTMANAGERREADONLY", "USETRANSLATIONTOOL", "USEINTEGRITYMANAGER", "ADMINISTERCACHES", "ADMINISTERCUBES", "ADMINISTERHISTORYLISTS", "ADMINISTERJOBS", "SCHEDULEADMIN", "ADMINISTERUSERCONNECTIONS", "USESECURITYFILTERMANAGER", "ASSIGNSECURITYROLES", "MONITORCHANGEJOURNAL", "ADMINBYPASSALLCHECKS", "CONFIGCACHES", "CONFIGAUDITING", "CONFIGCONNECTIONMAP", "CONFIGGOVERNING", "CONFIGLANGUAGE", "CONFIGPROJECTBASIC", "CONFIGPROJECTDATASOURCE", "CONFIGSECURITY", "CONFIGSTATS", "CONFIGSUBSCRIPTIONSETTINGS" , "DEFINESECURITYFILTER", "DUPLICATEPROJECT", "USEPROJECTSTATUSEDITOR", "IDLEPROJECT", "LOADPROJECTS", "USECACHEMONITOR", "USECUBEMONITOR", "HISTORYLISTMONITOR", "USEJOBMONITOR", "USEPROJECTMONITOR", "USESCHEDULEMONITOR", "USEUSERCONNMONITOR", "WEBADMIN"};

		boolean removeme = true;

		for (String verifiedToken : defaultTokens) {
			String output = "";
			for (int i = 0; i < arrSbTokenList.length; i++) {

				if(removeme){
					//printOut(arrSecurityRoleName[i] + "=" + arrSbTokenList[i]);
				}

				if(arrSbTokenList[i].toString().contains(";"+verifiedToken+";")){
					output += "S;;";
				} else {
					output += "N;;";
				}
			}
			removeme = false;
			printOut(output);
		}

	}
	
	public void listaEveryonePhysicalAddress(){
		// Recebe o resultado da consulta
		ResultSet membrosEveryonRS = executeCapture("LIST MEMBERS FOR USER GROUP \"Everyone\";");
		membrosEveryonRS.moveFirst();
		membrosEveryonRS = (ResultSet) membrosEveryonRS.getFieldValue(MEMBER_RESULTSET);
		membrosEveryonRS.getRowCount();
		
		// Verifica se existe algum resultado
		if (membrosEveryonRS.getRowCount() > 0) {
		
			// Anda pelo primeiro elemento do ResultSet membrosEveryonRS
			membrosEveryonRS.moveFirst();
			while (!membrosEveryonRS.isEof()) {
		
				// INÍCIO - Implementação sobre o resultset membrosEveryonRS
				String login = membrosEveryonRS.getFieldValueString(LOGIN);
				// Recebe o resultado da consulta
				ResultSet userPropertiesRS = (ResultSet) executeCapture("LIST PROPERTIES FOR USER \"" + login + "\";");
		
				// Verifica se existe algum resultado
				if (userPropertiesRS.getRowCount() > 0) {
		
					// Anda pelo primeiro elemento do ResultSet userPropertiesRS
					userPropertiesRS.moveFirst();
					while (!userPropertiesRS.isEof()) {
		
						// INÍCIO - Implementação sobre o resultset userPropertiesRS
						ResultSet physicalAddressRS = (ResultSet) userPropertiesRS.getFieldValue(18);
						physicalAddressRS.getRowCount();
		
						// Verifica se existe algum resultado
						if (physicalAddressRS.getRowCount() > 0) {
		
							// Anda pelo primeiro elemento do ResultSet physicalAddressRS
							physicalAddressRS.moveFirst();
							while (!physicalAddressRS.isEof()) {
		
								// INÍCIO - Implementação sobre o resultset physicalAddressRS
								printOut(physicalAddressRS.getFieldValueString(1));
								// FIM - Implementação sobre o resultset physicalAddressRS
		
								// Da continuidade a iteração com o ResultSet
								// physicalAddressRS
								physicalAddressRS.moveNext();
							}
						}
						
						// FIM - Implementação sobre o resultset userPropertiesRS
		
						// Da continuidade a iteração com o ResultSet
						// userPropertiesRS
						userPropertiesRS.moveNext();
					}
				}
				
				// FIM - Implementação sobre o resultset membrosEveryonRS
		
				// Da continuidade a iteração com o ResultSet
				// membrosEveryonRS
				membrosEveryonRS.moveNext();
			}
		}
		
	}
	
	public void listProjectMembers(){
		// Recebe o resultado da consulta
		ResultSet projMembersRS = executeCapture("LIST MEMBERS FOR USER GROUP \"PROJ\";");
		projMembersRS.moveFirst();
		projMembersRS = (ResultSet) projMembersRS.getFieldValue(MEMBER_RESULTSET);
		projMembersRS.getRowCount();
		
		// Verifica se existe algum resultado
		if (projMembersRS.getRowCount() > 0) {
		
			// Anda pelo primeiro elemento do ResultSet projMembersRS
			projMembersRS.moveFirst();
			while (!projMembersRS.isEof()) {
		
				// INÍCIO - Implementação sobre o resultset projMembersRS
				printOut(projMembersRS.getFieldValueString(LOGIN));
				// FIM - Implementação sobre o resultset projMembersRS
		
				// Da continuidade a iteração com o ResultSet
				// projMembersRS
				projMembersRS.moveNext();
			}
		}
	}
	
	public void gerarQueryCommandManagerAlterarConfiguracaoProjeto(){
		// Recebe o resultado da consulta
		ResultSet projectsRS = (ResultSet) executeCapture("LIST ALL PROJECTS;");
		
		// Verifica se existe algum resultado
		if (projectsRS.getRowCount() > 0) {
		
			// Anda pelo primeiro elemento do ResultSet projectsRS
			projectsRS.moveFirst();
			while (!projectsRS.isEof()) {
		
				// INÍCIO - Implementação sobre o resultset projectsRS
				try{
					
					// Recebe o resultado da consulta
					String nomeProjeto = projectsRS.getFieldValueString(NAME);
					ResultSet maxRS = executeCapture("LIST PROPERTIES FOR PROJECT CONFIGURATION IN PROJECT \"" + nomeProjeto + "\";");
					maxRS.moveFirst();
					maxRS = (ResultSet) maxRS.getFieldValue(GOVERNING_RESULT_SET);
					maxRS.getRowCount();
					// Verifica se existe algum resultado
					if (maxRS.getRowCount() > 0) {

						// Anda pelo primeiro elemento do ResultSet maxRS
						maxRS.moveFirst();
						while (!maxRS.isEof()) {

							// INÍCIO - Implementação sobre o resultset maxRS
							String consulta = "";
							
							consulta += "ALTER PROJECT CONFIGURATION";
							consulta += "INTERACTIVEJOBPERPROJECT " + maxRS.getFieldValueString(INTERACTIVEJOBPERPROJECT) + " ";
							consulta += "MAXCACHEUPDATESUBSCRIPTIONS " + maxRS.getFieldValueString(MAXCACHEUPDATESUBSCRIPTIONS) + " ";
							consulta += "MAXEMAILSUBSCRIPTIONS " + maxRS.getFieldValueString(MAXEMAILSUBSCRIPTIONS) + " ";
							consulta += "MAXEXECJOBSUSER " + maxRS.getFieldValueString(MAXEXECJOBSUSER) + " ";
							consulta += "MAXFILESUBSCRIPTIONS " + maxRS.getFieldValueString(MAXFILESUBSCRIPTIONS) + " ";
							consulta += "MAXHISTLISTSUBSCRIPTIONS " + maxRS.getFieldValueString(MAXHISTLISTSUBSCRIPTIONS) + " ";
							consulta += "MAXJOBSPROJECT " + maxRS.getFieldValueString(MAXJOBSPROJECT) + " ";
							consulta += "MAXJOBSUSERACCT " + maxRS.getFieldValueString(MAXJOBSUSERACCT) + " ";
							consulta += "MAXJOBSUSERSESSION " + maxRS.getFieldValueString(MAXJOBSUSERSESSION) + " ";
							consulta += "MAXMOBILESUBSCRIPTIONS " + maxRS.getFieldValueString(MAXMOBILESUBSCRIPTIONS) + " ";
							consulta += "MAXNOELEMROWS " + maxRS.getFieldValueString(MAXNOELEMROWS) + " ";
							consulta += "MAXNOINTRESULTROWS " + maxRS.getFieldValueString(MAXNOINTRESULTROWS) + " ";
							consulta += "MAXNOREPORTRESULTROWS " + maxRS.getFieldValueString(MAXNOREPORTRESULTROWS) + " ";
							consulta += "MAXPRINTSUBSCRIPTIONS " + maxRS.getFieldValueString(MAXPRINTSUBSCRIPTIONS) + " ";
							consulta += "MAXREPORTEXECTIME " + maxRS.getFieldValueString(MAXREPORTEXECTIME) + " ";
							consulta += "MAXSCHEDULEREPORTEXECTIME " + maxRS.getFieldValueString(MAXSCHEDULEREPORTEXECTIME) + " ";
							consulta += "MAXUSERSESSIONSPROJECT " + maxRS.getFieldValueString(MAXUSERSESSIONSPROJECT) + " ";
							consulta += "MAXFILESIZEIMPORT " + maxRS.getFieldValueString(MAX_FILE_SIZE_IMPORT) + " ";
							consulta += "MAXINTERACTIVESESSIONSPERUSER " + maxRS.getFieldValueString(MAX_INTERACTIVE_SESSIONS_PER_USER) + " ";
							consulta += "MAXNODATAMARTRESULTROWS " + maxRS.getFieldValueString(MAX_NO_DATA_MART_RESULTS_ROW) + " ";
							consulta += "MAXQUOTAIMPORT " + maxRS.getFieldValueString(MAX_QUOTA_IMPORT) + " ";
							consulta += "SQLGENERATIONMEMORY " + maxRS.getFieldValueString(SQLGENERATIONMEMORY) + " ";
							consulta += "WAITTIMEFORPROMPTANSWERS " + maxRS.getFieldValueString(WAITTIMEFORPROMPTANSWERS) + " ";
							consulta += "WAREHOUSEEXECUTIONTIME " + maxRS.getFieldValueString(WAREHOUSEEXECUTIONTIME) + " ";
							consulta += "IN PROJECT \"" + nomeProjeto + "\";";
							
							printOut(consulta);
							// FIM - Implementação sobre o resultset maxRS

							// Da continuidade a iteração com o ResultSet
							// maxRS
							maxRS.moveNext();
						}
					}
					
				}catch(Exception e){}
				
				// FIM - Implementação sobre o resultset projectsRS
		
				// Da continuidade a iteração com o ResultSet
				// projectsRS
				projectsRS.moveNext();
			}
		}
	}
	
	public void getMaxNumber2(){
		// Recebe o resultado da consulta
		ResultSet maxRS = (ResultSet) executeCapture("LIST PROPERTIES FOR PROJECT CONFIGURATION IN PROJECT \"AIN - Analise de Interrupcoes\";");

		// Verifica se existe algum resultado
		if (maxRS.getRowCount() > 0) {

			// Anda pelo primeiro elemento do ResultSet maxRS
			maxRS.moveFirst();
			while (!maxRS.isEof()) {

				// INÍCIO - Implementação sobre o resultset maxRS
				printOut(maxRS.getFieldValueString(MAXNOREPORTRESULTROWS));
				// FIM - Implementação sobre o resultset maxRS

				// Da continuidade a iteração com o ResultSet
				// maxRS
				maxRS.moveNext();
			}
		}
	}
	
	public void getMaxNumber3(){
		// Recebe o resultado da consulta
		String nomeProjeto = "AIN - Analise de Interrupcoes";
		ResultSet maxRS = executeCapture("LIST PROPERTIES FOR PROJECT CONFIGURATION IN PROJECT \"" + nomeProjeto + "\";");
		maxRS.moveFirst();
		maxRS = (ResultSet) maxRS.getFieldValue(GOVERNING_RESULT_SET);
		maxRS.getRowCount();
		// Verifica se existe algum resultado
		if (maxRS.getRowCount() > 0) {

			// Anda pelo primeiro elemento do ResultSet maxRS
			maxRS.moveFirst();
			while (!maxRS.isEof()) {

				// INÍCIO - Implementação sobre o resultset maxRS
				String consulta = "";
				
				consulta += "ALTER PROJECT CONFIGURATION";
				consulta += "INTERACTIVEJOBPERPROJECT " + maxRS.getFieldValueString(INTERACTIVEJOBPERPROJECT) + " ";
				consulta += "MAXCACHEUPDATESUBSCRIPTIONS " + maxRS.getFieldValueString(MAXCACHEUPDATESUBSCRIPTIONS) + " ";
				consulta += "MAXEMAILSUBSCRIPTIONS " + maxRS.getFieldValueString(MAXEMAILSUBSCRIPTIONS) + " ";
				consulta += "MAXEXECJOBSUSER " + maxRS.getFieldValueString(MAXEXECJOBSUSER) + " ";
				consulta += "MAXFILESUBSCRIPTIONS " + maxRS.getFieldValueString(MAXFILESUBSCRIPTIONS) + " ";
				consulta += "MAXHISTLISTSUBSCRIPTIONS " + maxRS.getFieldValueString(MAXHISTLISTSUBSCRIPTIONS) + " ";
				consulta += "MAXJOBSPROJECT " + maxRS.getFieldValueString(MAXJOBSPROJECT) + " ";
				consulta += "MAXJOBSUSERACCT " + maxRS.getFieldValueString(MAXJOBSUSERACCT) + " ";
				consulta += "MAXJOBSUSERSESSION " + maxRS.getFieldValueString(MAXJOBSUSERSESSION) + " ";
				consulta += "MAXMOBILESUBSCRIPTIONS " + maxRS.getFieldValueString(MAXMOBILESUBSCRIPTIONS) + " ";
				consulta += "MAXNOELEMROWS " + maxRS.getFieldValueString(MAXNOELEMROWS) + " ";
				consulta += "MAXNOINTRESULTROWS " + maxRS.getFieldValueString(MAXNOINTRESULTROWS) + " ";
				consulta += "MAXNOREPORTRESULTROWS " + maxRS.getFieldValueString(MAXNOREPORTRESULTROWS) + " ";
				consulta += "MAXPRINTSUBSCRIPTIONS " + maxRS.getFieldValueString(MAXPRINTSUBSCRIPTIONS) + " ";
				consulta += "MAXREPORTEXECTIME " + maxRS.getFieldValueString(MAXREPORTEXECTIME) + " ";
				consulta += "MAXSCHEDULEREPORTEXECTIME " + maxRS.getFieldValueString(MAXSCHEDULEREPORTEXECTIME) + " ";
				consulta += "MAXUSERSESSIONSPROJECT " + maxRS.getFieldValueString(MAXUSERSESSIONSPROJECT) + " ";
				consulta += "MAXFILESIZEIMPORT " + maxRS.getFieldValueString(MAX_FILE_SIZE_IMPORT) + " ";
				consulta += "MAXINTERACTIVESESSIONSPERUSER " + maxRS.getFieldValueString(MAX_INTERACTIVE_SESSIONS_PER_USER) + " ";
				consulta += "MAXNODATAMARTRESULTROWS " + maxRS.getFieldValueString(MAX_NO_DATA_MART_RESULTS_ROW) + " ";
				consulta += "MAXQUOTAIMPORT " + maxRS.getFieldValueString(MAX_QUOTA_IMPORT) + " ";
				consulta += "SQLGENERATIONMEMORY " + maxRS.getFieldValueString(SQLGENERATIONMEMORY) + " ";
				consulta += "WAITTIMEFORPROMPTANSWERS " + maxRS.getFieldValueString(WAITTIMEFORPROMPTANSWERS) + " ";
				consulta += "WAREHOUSEEXECUTIONTIME " + maxRS.getFieldValueString(WAREHOUSEEXECUTIONTIME) + " ";
				consulta += "IN PROJECT \"" + nomeProjeto + "\";";
				
				printOut(consulta);
				// FIM - Implementação sobre o resultset maxRS

				// Da continuidade a iteração com o ResultSet
				// maxRS
				maxRS.moveNext();
			}
		}
	}

}
