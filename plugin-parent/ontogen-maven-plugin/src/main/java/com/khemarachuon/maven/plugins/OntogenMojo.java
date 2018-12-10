package com.khemarachuon.maven.plugins;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Generates Java ontology classes for use with Jena.
 * 
 * TODO: review https://github.com/apache/jena/blob/master/jena-cmds/src/main/java/jena/schemagen.java
 */
@Mojo(name="ontogen", defaultPhase=LifecyclePhase.GENERATE_SOURCES, threadSafe=true)
public class OntogenMojo extends AbstractMojo {

	public static final String GENERATOR_NAME = "ontogen";

	/**
	 * Maven project parameters
	 */
	@Parameter(defaultValue="${project}", readonly=true, required=true)
	private MavenProject project;

	/**
	 * List of source paths to generate
	 */
	@Parameter(defaultValue="**/*.rdf")
	private List<String> inputs;

	/**
	 * Directory to place generated Java classes
	 */
	@Parameter(defaultValue="${generated.sources.directory}")
	private String outputDirectory;

	/**
	 * Package to generate Java classes in
	 */
	@Parameter(name="package", defaultValue="ontology", required=false)
	private String packagePath;

	/**
	 * Map of source URIs to generated Java class names
	 */
	@Parameter
	private Map<String, String> classNames;

	/**
	 * Map of source URIS to generated namespace URIs
	 */
	@Parameter
	private Map<String, String> namespaces;

	/* (non-Javadoc)
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
//		TimeZone.setDefault(TimeZone.getTimeZone("Universal/UTC"));
		
		String input = "src/main/resources/ontology/dctype-2012-06-14.rdf";
		input = "D:\\work\\installed\\.home\\workspace\\machinelearning\\parent\\ontologies\\src\\main\\resources\\ontology\\dctype-2012-06-14.rdf";
		input = "D:\\work\\installed\\.home\\workspace\\machinelearning\\parent\\ontologies\\src\\main\\resources\\ontology\\foaf-0.1.rdf";
		final File inputFile = new File(input);
		final String className = "FOAF2";
		final String namespace = "http://xmlns.com/foaf/0.1/";
		packagePath = "com.khemarachuon.machinelearning.ontology";
		outputDirectory = "target";
		final File outputFile = new File(getFile(outputDirectory), String.format("%s.java", className));
		
		final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		final XPathFactory xpathFactory = XPathFactory.newInstance();
		final TransformerFactory transformerFactory = TransformerFactory.newInstance();
		if (getLog().isDebugEnabled()) {
			getLog().debug(String.format("javax.xml.parsers.DocumentBuilderFactory implementation: %s", documentBuilderFactory.getClass().getName()));
			getLog().debug(String.format("javax.xml.xpath.XPathFactory implementation: %s", xpathFactory.getClass().getName()));
			getLog().debug(String.format("javax.xml.transform.TransformerFactory implementation: %s", transformerFactory.getClass().getName()));
		}
		
		try {
			final Templates rdf2JavaTemplate = transformerFactory.newTemplates(
					new StreamSource(getClass().getResourceAsStream("/xsl/rdf-to-java.xsl")));
			final Transformer transformer = rdf2JavaTemplate.newTransformer();
			transformer.setParameter("generator", GENERATOR_NAME);
			transformer.setParameter("sourceUri", inputFile.toURI());
			transformer.setParameter("package", packagePath);
			transformer.setParameter("className", className);
			transformer.setParameter("namespace", namespace);
			
			final Document document = documentBuilderFactory.newDocumentBuilder().parse(inputFile);
			final DOMSource documentSource = new DOMSource(document);
//			final XPath xpath = xpathFactory.newXPath();
//			final String evaluate = xpath.evaluate("/*:RDF", documentSource);
			
			getLog().info(String.format("Ontogen generating source: %s to output: %s", inputFile.toURI(), outputFile.toURI()));
			try (final FileOutputStream outputStream = new FileOutputStream(outputFile)) {
				transformer.transform(documentSource, new StreamResult(outputStream));
			}
		} catch (TransformerConfigurationException e) {
			throw new MojoFailureException("Failed to create xml transformer", e);
		} catch (ParserConfigurationException e) {
			throw new MojoFailureException("Failed to create xml parser", e);
		} catch (SAXException | IOException e) {
			throw new MojoExecutionException(String.format("Failed to parse: %s", input), e);
//		} catch (XPathExpressionException e) {
//			throw new MojoExecutionException(String.format("Failed to execute xpath in document: %s", input), e);
		} catch (TransformerException e) {
			throw new MojoExecutionException(String.format("Failed to transform: %s", input), e);
		}
	}

	private File getFile(final String path) {
		final File file = new File(path);
		if (file.isAbsolute()) {
			return file;
		} else {
//			return new File(project.getBasedir(), path);
			return new File(path).getAbsoluteFile();
		}
	}

	public static void main(String[] args) throws MojoExecutionException, MojoFailureException {
		new OntogenMojo().execute();
	}
}
