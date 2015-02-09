package de.unima.dws.alex.onto2graph;

import com.interdataworking.mm.alg.MapPair;
import com.interdataworking.mm.alg.Match;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.w3c.rdf.model.Model;
import org.w3c.rdf.model.ModelException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mueller on 26/01/15.
 */
public class Matcher {

    /**
     * Matches two OWL ontologies based on an input matching
     * @param onto1 path to the source ontology
     * @param onto2 path to the target ontology
     * @param initialMatching initial matching
     * @param formula formula used see similarity flooding paper for more detail
     * @param flow_graph_type flow graph type used see similarity flooding paper for more detail
     * @return List of mappings
     * @throws OWLOntologyCreationException
     * @throws ModelException
     */
    public static MapPair[] structMatch(String onto1, String onto2, List<MapPair> initialMatching,  boolean[] formula,int flow_graph_type) throws OWLOntologyCreationException, ModelException {
        IRI onto1_iri = IRI.create(new File(onto1));
        OWLOntology owl_onto1 = OWLManager.createOWLOntologyManager().loadOntology(onto1_iri);

        IRI onto2_iri = IRI.create(new File(onto2));
        OWLOntology owl_onto2 = OWLManager.createOWLOntologyManager().loadOntology(onto2_iri);

        return structMatch(owl_onto1,owl_onto2,initialMatching,formula,flow_graph_type);
    }

    /**
     * Matches two OWL ontologies based on an input matching
     * @param owl_onto1 OWL Ontology object of the source ontology
     * @param owl_onto2 OWL Ontology object of the target ontology
     * @param initialMatching initial matching
     * @param formula formula used see similarity flooding paper for more detail
     * @param flow_graph_type flow graph type used see similarity flooding paper for more detail
     * @return
     * @throws ModelException
     */
    public static MapPair[] structMatch(OWLOntology owl_onto1, OWLOntology owl_onto2, List<MapPair> initialMatching,  boolean[] formula,int flow_graph_type ) throws ModelException {

        Model onto1_model = Onto2GraphConverter.convertToGraph(owl_onto1);
        Model onto2_model = Onto2GraphConverter.convertToGraph(owl_onto2);

        Match sf = new Match();
        //set configuration
        sf.formula = formula;
        sf.FLOW_GRAPH_TYPE = flow_graph_type;
        sf.TEST = false;

        ArrayList<MapPair> addedMatchings = new ArrayList<MapPair>();

        addedMatchings.addAll(initialMatching);

        List<MapPair> initialMatchingWithDT = addDataTypeMatchings(owl_onto1,owl_onto2,addedMatchings);

        MapPair[] result = sf.getMatch(onto1_model, onto2_model, initialMatchingWithDT);
        MapPair.sort(result);

        return result;
    }

    public static List<MapPair> addDataTypeMatchings(OWLOntology onto1, OWLOntology onto2, List<MapPair> dataTypeMatching) {


        onto1.getDatatypesInSignature().forEach(owlDatatype -> {
            onto2.getDatatypesInSignature().forEach(owlDatatype2 -> {
                if (owlDatatype.equals(owlDatatype2)) {
                    dataTypeMatching.add(new MapPair(owlDatatype.getIRI().toString(), owlDatatype2.getIRI().toString(), 1.0));
                }
                ;
            });
        });
        return dataTypeMatching;
    }
}
