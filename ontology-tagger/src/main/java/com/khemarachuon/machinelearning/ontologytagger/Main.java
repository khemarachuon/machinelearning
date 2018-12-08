package com.khemarachuon.machinelearning.ontologytagger;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.tdb.base.file.Location;
import org.apache.jena.vocabulary.VCARD4;

public class Main {
	
	public static final String DOCUMENT_NAMED_MODEL_URI_TEMPLATE = "http://khemarachuon.com/ontology/%s/%s/%s";
	public static final String DOCUMENT_ENTITY_URI_TEMPLATE = "http://khemarachuon.com/ontology/instance/%s/%s/%s#%s";
	
	public static void main(String[] args) {
		String source = "localdisk";
		String documentId = "Ginger_the_Jiraffe.xhtml";
		
		Dataset dataset0 = TDBFactory.createDataset(Location.mem("dataset-0"));
		
//		OntModel schemaModel = ModelFactory.createOntologyModel();
		Model schemaModel = ModelFactory.createDefaultModel();
		schemaModel.setNsPrefixes(PrefixMapping.Factory.create()
				.setNsPrefix("kc-schema", "http://khemarachuon.com/ontology/schema/")
				.lock());
		Model instanceModel = ModelFactory.createDefaultModel();
		instanceModel.setNsPrefixes(PrefixMapping.Factory.create()
				.setNsPrefix("kc-instance", "http://khemarachuon.com/ontology/instance/")
				.lock());
		
		Property livesInProperty = schemaModel.createProperty("http://khemarachuon.com/ontology/schema/property/person#livesIn");
		
		try {
			Resource gingerPerson = instanceModel.createResource(String.format(DOCUMENT_ENTITY_URI_TEMPLATE, source, documentId, "person", "ginger"), FOAF.Person);
			Resource mickeyPerson = instanceModel.createResource(String.format(DOCUMENT_ENTITY_URI_TEMPLATE, source, documentId, "person", "mickey"), FOAF.Person);
			Resource leoPerson = instanceModel.createResource(String.format(DOCUMENT_ENTITY_URI_TEMPLATE, source, documentId, "person", "leo"), FOAF.Person);
			Resource eddiePerson = instanceModel.createResource(String.format(DOCUMENT_ENTITY_URI_TEMPLATE, source, documentId, "person", "eddie"), FOAF.Person);
			
			Resource anonZebraPerson = instanceModel.createResource(String.format(DOCUMENT_ENTITY_URI_TEMPLATE, source, documentId, "person", "zebra-0"), FOAF.Person);
			
			instanceModel.add(gingerPerson, FOAF.knows, mickeyPerson);
			instanceModel.add(gingerPerson, FOAF.knows, leoPerson);
			
			instanceModel.add(anonZebraPerson, FOAF.knows, eddiePerson);
			
			Resource gingerAddress = instanceModel.createResource(String.format(DOCUMENT_ENTITY_URI_TEMPLATE, source, documentId, "address", "ginger"), VCARD4.Address);
			// http://sws.geonames.org/10861316/about.rdf
			Resource kenyaFeature = instanceModel.createResource("http://sws.geonames.org/10861316/");
			
			instanceModel.add(gingerPerson, livesInProperty, kenyaFeature);
			instanceModel.add(gingerPerson, VCARD4.hasAddress, gingerAddress);
			instanceModel.add(gingerAddress, VCARD4.country_name, "Kenya");
			
			
			Model kenyaModel = ModelFactory.createDefaultModel();
			kenyaModel.read("http://sws.geonames.org/10861316/about.rdf");
			
			Model unionModel = ModelFactory.createDefaultModel();
			unionModel.add(schemaModel);
			unionModel.read(VCARD4.getURI());
			unionModel.add(kenyaModel);
			
			System.out.println("Starting inference....");
			InfModel inferenceModel = ModelFactory.createInfModel(ReasonerRegistry.getOWLReasoner(), unionModel, instanceModel);
			inferenceModel.setNsPrefixes(PrefixMapping.Factory.create()
					.setNsPrefix("kc-schema", "http://khemarachuon.com/ontology/schema/")
					.setNsPrefix("kc-instance", "http://khemarachuon.com/ontology/instance/")
					.lock());
			if (!inferenceModel.validate().isValid()) {
				throw new RuntimeException("Invalid inference");
			}
			System.out.println("Completed inference....");
			
			dataset0.addNamedModel(String.format(DOCUMENT_NAMED_MODEL_URI_TEMPLATE, "schema", source, documentId), schemaModel);
			dataset0.addNamedModel(String.format(DOCUMENT_NAMED_MODEL_URI_TEMPLATE, "instance", source, documentId), instanceModel);
			dataset0.addNamedModel(String.format(DOCUMENT_NAMED_MODEL_URI_TEMPLATE, "inference", source, documentId), inferenceModel);
			
//			RDFDataMgr.write(System.err, inferenceModel, Lang.NQUADS);
//			RDFDataMgr.write(System.err, dataset0, Lang.NQUADS);
			
			Query query = QueryFactory.create(new StringBuilder()
					.append("PREFIX foaf:        <http://xmlns.com/foaf/0.1/>\n")
					.append("PREFIX gn:          <http://www.geonames.org/ontology#>\n")
					.append("PREFIX kc-schema:   <http://khemarachuon.com/ontology/schema/property/person#>")
					.append("PREFIX kc-instance: <http://khemarachuon.com/ontology/instance/localdisk/Ginger_the_Jiraffe.xhtml/>\n")
					.append("SELECT ?personA ?country WHERE {\n")
//					.append("  ?personA foaf:knows ?personB\n")
					.append("  ?personA kc-schema:livesIn ?country . \n")
					.append("  ?country gn:name ?countryName \n")
					.append("}")
					.toString());
			QueryExecution queryExecution = QueryExecutionFactory.create(query, inferenceModel);
			ResultSet resultSet = queryExecution.execSelect();
			resultSet.forEachRemaining(rs -> {
				System.out.println(rs);
			});
		} finally {
			schemaModel.close();
		}
	}
}
