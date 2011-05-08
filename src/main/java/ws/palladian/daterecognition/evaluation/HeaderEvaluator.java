package ws.palladian.daterecognition.evaluation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import ws.palladian.daterecognition.DateGetterHelper;
import ws.palladian.daterecognition.dates.ExtractedDate;
import ws.palladian.daterecognition.dates.MetaDate;
import ws.palladian.daterecognition.searchengine.DBExport;
import ws.palladian.daterecognition.searchengine.DataSetHandler;
import ws.palladian.daterecognition.technique.HeadDateGetter;
import ws.palladian.daterecognition.technique.HeadDateRater;
import ws.palladian.daterecognition.technique.PageDateType;
import ws.palladian.daterecognition.technique.TechniqueDateGetter;
import ws.palladian.daterecognition.technique.TechniqueDateRater;
import ws.palladian.daterecognition.technique.URLDateGetter;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.date.DateArrayHelper;
import ws.palladian.retrieval.DocumentRetriever;


public class HeaderEvaluator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		
		
		String file = "data/evaluation/daterecognition/datasets/headdataset.txt";
		//DataSetHandler.addCloumn(EvaluationHelper.HEADEVAL, "mod2", "INT", "-10");
		
		TechniqueDateGetter<MetaDate> dg = new HeadDateGetter();
		TechniqueDateRater<MetaDate> dr = new HeadDateRater(PageDateType.publish);
		
		//TestHeadDateRater testDR = new TestHeadDateRater(PageDateType.publish);
		
		
		String pub = "pub1";
		String mod = "mod1";
		String path;
		int des = 3;
		switch(des){
		case 0:
			String in = "D:/_Uni/_semester16/dataset/urlSet18.htm";
			String out = "D:/_Uni/_semester16/dataset/HeadUrlSet18.htm";
			//countHeadURls(in, out, (HeadDateGetter)dg);
			createHeadUrlList(in, out, (HeadDateGetter)dg);
			break;
		case 1:
			path = "data/evaluation/daterecognition/datasets/headdataset.txt";
			DataSetHandler.createSearchDatesAndDownload("headdateset", path);
			break;
		case 2:
			path = "data/evaluation/daterecognition/datasets/headdataset.txt";
			DataSetHandler.clearTable("headdateset", path);
			break;
		case 3:
			evaluate(pub, DBExport.PUB_DATE, dg, dr, file);
			evaluate(mod, DBExport.MOD_DATE, dg, dr, file);
			break;
		} 
		

		/*
		evaluate("pub3", DBExport.PUB_DATE, dg, dr, file);
		evaluate("mod3", DBExport.MOD_DATE, dg, dr, file);
		*/
		
		/*
		String in1 = "data/evaluation/daterecognition/datasets/headdataset9.txt";
		String in2 = "data/evaluation/daterecognition/datasets/urldataset.txt";
		mergeUrlsets(in1, in2, file);
		*/
		
		//HashMap<Byte, Integer[]> parameter = new HashMap<Byte, Integer[]>();
		//parameter.put(testDR.MOD_DATE_PARAMETER, null);
		//Integer[] measure = {DateComparator.MEASURE_MIN};
		//parameter.put(testDR.MEASURE_PARAMETER, measure);
		/*parameter.put(testDR.YOUNGEST_PARAMERT, null);
		testDR.setParameter(parameter);
		evaluate("mod2", DBExport.MOD_DATE, dg, testDR);
		String[] round = {"mod0", "mod1", "mod2"};
		EvaluationHelper.calculateOutput(round, EvaluationHelper.HEADEVAL);
		*/
		

		
		
		
		System.out.println(pub);
		System.out.println("AFR: " + EvaluationHelper.count(file, pub, EvaluationHelper.HEADEVAL, DataSetHandler.AFR));
		System.out.println("ARD: " + EvaluationHelper.count(file, pub, EvaluationHelper.HEADEVAL, DataSetHandler.ARD));
		System.out.println("AFW: " + EvaluationHelper.count(file, pub, EvaluationHelper.HEADEVAL, DataSetHandler.AFW));
		System.out.println("ANF: " + EvaluationHelper.count(file, pub, EvaluationHelper.HEADEVAL, DataSetHandler.ANF));
		System.out.println("AWD: " + EvaluationHelper.count(file, pub, EvaluationHelper.HEADEVAL, DataSetHandler.AWD));
				
		System.out.println(mod);
		System.out.println("AFR: " + EvaluationHelper.count(file, mod, EvaluationHelper.HEADEVAL, DataSetHandler.AFR));
		System.out.println("ARD: " + EvaluationHelper.count(file, mod, EvaluationHelper.HEADEVAL, DataSetHandler.ARD));
		System.out.println("AFW: " + EvaluationHelper.count(file, mod, EvaluationHelper.HEADEVAL, DataSetHandler.AFW));
		System.out.println("ANF: " + EvaluationHelper.count(file, mod, EvaluationHelper.HEADEVAL, DataSetHandler.ANF));
		System.out.println("AWD: " + EvaluationHelper.count(file, mod, EvaluationHelper.HEADEVAL, DataSetHandler.AWD));
		
	}

	private static <T,V> void evaluate(String round,int pub_mod, TechniqueDateGetter<T> dg, TechniqueDateRater<V> dr, String file){
		String table = EvaluationHelper.HEADEVAL;
		int rnf = 0;
		int ff= 0;
		int wnf= 0;
		int rf= 0;
		int wf = 0;
		int counter=0;
		int compare;
		
		HashMap<String, DBExport> set = EvaluationHelper.readFile(file);
		DocumentRetriever crawler = new DocumentRetriever();
		
		for(Entry<String, DBExport> e : set.entrySet()){
			dg.reset();
			T bestDate = null;
			String bestDateString ="";
			String url =e.getValue().get(DBExport.URL);
			dg.setUrl(url);
			//System.out.println(url);
			if(table.equalsIgnoreCase(EvaluationHelper.CONTENTEVAL) || table.equalsIgnoreCase(EvaluationHelper.STRUCTEVAL) || table.equalsIgnoreCase(EvaluationHelper.HEADEVAL)){
				String path = e.getValue().get(DBExport.PATH);
				//System.out.println(path);
				dg.setDocument(crawler.getWebDocument(path));
			}else{
				
				dg.setUrl(url);
			}
			
			System.out.print("get dates... ");
			StopWatch timer = new StopWatch();
			ArrayList<T> list = dg.getDates();
			timer.stop();
			timer.getElapsedTimeString(true);
			list = DateArrayHelper.removeNull(list);
			
			if(list.size() > 0){
				
				ArrayList<T> filteredDates = DateArrayHelper.filter(list, DateArrayHelper.FILTER_FULL_DATE);
				filteredDates = DateArrayHelper.filter(filteredDates, DateArrayHelper.FILTER_IS_IN_RANGE);
				
				if(dg instanceof URLDateGetter){
					filteredDates = DateArrayHelper.filter(list, DateArrayHelper.FILTER_IS_IN_RANGE);
				}
				
				
				if(filteredDates.size()>0){
						
					//System.out.print("rate dates... ");
					dr.rate((ArrayList<V>) filteredDates);
					//System.out.print("best date... ");
					bestDate = (T) dr.getBestDate();
					if(bestDate != null){
						bestDateString = ((ExtractedDate) bestDate).getNormalizedDate(true);
					}
				}
			}
			//System.out.println("compare...");
			
			compare = EvaluationHelper.compareDate(bestDate, e.getValue(),pub_mod);
			ExtractedDate date;
			String dbExportDateString;
			if(pub_mod == DBExport.PUB_DATE){
				date = DateGetterHelper.findDate(e.getValue().getPubDate());
				dbExportDateString =" - pubDate:" ;
			}else{
				date = DateGetterHelper.findDate(e.getValue().getModDate());
				dbExportDateString =" - modDate:" ;
			}
			
			if(date!=null){
				dbExportDateString +=  date.getNormalizedDateString();
			}
			
			//System.out.print(compare + " bestDate:" + bestDateString + dbExportDateString);
			
			switch(compare){
				case DataSetHandler.AFW:
					wf++;
					System.out.println(url);
					System.out.println(compare + " bestDate:" + bestDateString + dbExportDateString);
					//System.out.println("-------------------------------------------------------");
					break;
				case DataSetHandler.ANF:
					System.out.println(url);
					System.out.println(compare + " bestDate:" + bestDateString + dbExportDateString);
					//System.out.println("-------------------------------------------------------");
					wnf++;
					break;
				case DataSetHandler.AWD:
					System.out.println(url);
					System.out.println(compare + " bestDate:" + bestDateString + dbExportDateString);
					//System.out.println("-------------------------------------------------------");
					ff++;
					break;
				case DataSetHandler.ARD:
					rnf++;
					break;
				case DataSetHandler.AFR:
					rf++;
					break;
					
			}
			
			DataSetHandler.writeInDB(table, e.getValue().getUrl(), compare, round);
			counter++;
			
			//System.out.println();
			System.out.println("all: " + counter + " RF: " + rf + " RNF: " + rnf + " WF: " + wf + " FF: " + ff + " WNF: " + wnf);
			System.out.println("---------------------------------------------------------------------");
			
		}
		//System.out.println("all: " + counter + " RF: " + rf + " RNF: " + rnf + " WF: " + wf + " FF: " + ff + " WNF: " + wnf);
	}

	
	
	private static void countHeadURls(String in, String out, HeadDateGetter dg){
		HashMap<String, DBExport> set = EvaluationHelper.readFile(in);
		ArrayList<DBExport> headSet = new ArrayList<DBExport>();
		DocumentRetriever c = new DocumentRetriever();
		int index=0;
		for(Entry<String, DBExport> e : set.entrySet()){
			System.out.println(index + ": " + e.getKey());
			dg.setDocument(c.getWebDocument(e.getValue().get(DBExport.PATH)));
			ArrayList<MetaDate> dates = dg.getDates();
			if(dates.size() != 0){
				headSet.add(e.getValue());
			}
			index++;
		}
		
		try{
			File file = new File(out);
			FileWriter fw = new FileWriter(file);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write("url *;_;* path *;_;* pub_date *;_;* pub_sureness *;_;* mod_date *;_;* mod_sureness *;_;* google_date *;_;* hakia_date *;_;* ask_date *;_;* header_last_mod *;_;* header_date *;_;* down_date \n");
			for(int i=0; i< headSet.size(); i++){
				bw.write(headSet.get(i).toString() + "\n");
			}
			bw.close();
			fw.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private static void createHeadUrlList(String in, String out, HeadDateGetter dg){
		ArrayList<String> headSet = new ArrayList<String>();
		try{
			
			File file = new File(in);
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			String line;
			DocumentRetriever c = new DocumentRetriever();
			int lineindex=0;
			while((line=br.readLine())!=null){
				if(lineindex>1){
					int indexStart = line.indexOf('>');
					int indexEnd = line.indexOf("</a>");

					if(indexStart > -1 && indexEnd > -1){
						String url = line.substring(indexStart + 1, indexEnd);
						System.out.println(lineindex + ": " + url);
						dg.setDocument(c.getWebDocument(url));
						ArrayList<MetaDate> dates = dg.getDates();
						if(!dates.isEmpty()){
							System.out.println("+");
							headSet.add(line);
						}
					}
				}
				
				lineindex++;
			}
			br.close();
			fr.close();
		}catch(IOException e){
			
		}
		
		try{
			File file = new File(out);
			FileWriter fw = new FileWriter(file);
			BufferedWriter bw = new BufferedWriter(fw);
			
			for(int i=0; i<headSet.size(); i++){
				bw.write(headSet.get(i)+ "\n");
			}
			
			bw.close();
			fw.close();
		}catch(Exception e){
			
		}
	}
	
	private static void mergeUrlsets(String in1, String in2, String out){
			
			HashMap<String, DBExport> set1 = EvaluationHelper.readFile(in1);
			HashMap<String, DBExport> set2 = EvaluationHelper.readFile(in2);
			System.out.println(set1.size());
			System.out.println(set2.size());
			HashMap<String, DBExport> merged = new HashMap<String, DBExport>();
			merged.putAll(set1);
			merged.putAll(set2);
			String separator = DataSetHandler.SEPARATOR;
			File file = new File(out);
			try{
				FileWriter outw = new FileWriter(file, false);
				BufferedWriter bw = new BufferedWriter(outw);
				bw.write("url *;_;* path *;_;* pub_date *;_;* pub_sureness *;_;* mod_date *;_;* mod_sureness *;_;* google_date *;_;* hakia_date *;_;* ask_date *;_;* header_last_mod *;_;* header_date *;_;* down_date \n");
				DocumentRetriever c = new DocumentRetriever();
				for(Entry<String, DBExport>e : merged.entrySet()){
					HeadDateGetter dg = new HeadDateGetter();
					
					dg.setDocument(c.getWebDocument(e.getValue().get(DBExport.PATH)));
					
					ArrayList<MetaDate> headDates = dg.getDates();
					System.out.println(headDates.size());					
					if(headDates.size()>0){
					
						String write = e.getValue().getUrl() + separator
						+ e.getValue().getFilePath() + separator
						+ e.getValue().getPubDate() + separator
						+ String.valueOf(e.getValue().isPubSureness()) + separator
						+ e.getValue().getModDate() + separator
						+ String.valueOf(e.getValue().isModSureness()) + separator
						+ e.getValue().getGoogleDate() + separator
						+ e.getValue().getHakiaDate() + separator
						+ e.getValue().getAskDate() + separator
						+ e.getValue().getLastModDate() + separator
						+ e.getValue().getDateDate() + separator
						+ e.getValue().getActDate();
						bw.write(write + "\n");
					
						System.out.println(write);
					}
				}	
				bw.close();
				outw.close();
			} catch(Exception e){
				e.printStackTrace();
			}
		}
	
	
}
