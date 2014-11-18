package com.copel.javaprocedurecmd;

import java.util.HashMap;
import java.util.Iterator;

import com.copel.cmdutil.ResultSet;

@SuppressWarnings({"rawtypes", "unchecked"})
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
	
	public void listMembersForUserGroupEveryone(){
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

				if(loginUsuarioRS.getFieldValueString(IS_GROUP).equals("false")){
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
	
	public void excluirProjetoPadraoCopel(String nomeProjeto){
		// String nomeProjeto = "ING - Gestão de Indicadores";
		// String descricaoProjeto = "Aplicativo BI para análise e acompanhamento dos indicadores estratégicos da Copel";
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
	
	public void testeTemplate(){
		// Recebe o resultado da consulta
		ResultSet pastasRS = executeCapture("consulta");
		pastasRS.moveFirst();
		pastasRS = (ResultSet) pastasRS.getFieldValue(MEMBER_RESULTSET);
		pastasRS.getRowCount();

		// Verifica se existe algum resultado
		if (pastasRS.getRowCount() > 0) {

			// Anda pelo primeiro elemento do ResultSet pastasRS
			pastasRS.moveFirst();
			while (!pastasRS.isEof()) {

				if (pastasRS.getFieldValueString(IS_GROUP).equals("false")) {
					// INÍCIO - Implementação sobre o resultset pastasRS
					printOut(pastasRS.getFieldValueString(NAME));
					// FIM - Implementação sobre o resultset pastasRS
				}

				// Da continuidade a iteração com o ResultSet
				// pastasRS
				pastasRS.moveNext();
			}
		}
	}
	
	public void navegarRecursivo(String navegarRecursivo){
		
		
	}
	
}
