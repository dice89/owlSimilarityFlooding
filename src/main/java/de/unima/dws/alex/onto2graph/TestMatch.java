package de.unima.dws.alex.onto2graph;

import com.interdataworking.mm.alg.MapPair;
import com.interdataworking.mm.alg.Match;
import com.wcohen.ss.JaroWinkler;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.w3c.rdf.model.ModelException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * Test class to demonstrate Usage
 * Created by mueller on 26/01/15.
 */
public class TestMatch {

    public static void main(String args[]) throws OWLOntologyCreationException, ModelException {

        IRI cmt_iri = IRI.create(new File("cmt.owl"));
        OWLOntology cmt = OWLManager.createOWLOntologyManager().loadOntology(cmt_iri);


        IRI conf_iri = IRI.create(new File("Conference.owl"));
        OWLOntology conf = OWLManager.createOWLOntologyManager().loadOntology(conf_iri);

        List<MapPair> initialMapping = createInitialStringMappingJaroWinkler(cmt, conf);

        MapPair[] result = Matcher.structMatch("cmt.owl", "Conference.owl", initialMapping, Match.FORMULA_TTT, Match.FG_PRODUCT);
        Match.dump(result);


    }

    public static List<MapPair> createInitialStringMappingJaroWinkler(OWLOntology onto1, OWLOntology onto2) {
        List<MapPair> intialMapping = new ArrayList<MapPair>();

        onto1.getDatatypesInSignature().forEach(owlDatatype -> {
            onto2.getDatatypesInSignature().forEach(owlDatatype2 -> {
                if (owlDatatype.equals(owlDatatype2)) {
                    intialMapping.add(new MapPair(owlDatatype.getIRI().toString(), owlDatatype2.getIRI().toString(), 1.0));
                }
                ;
            });
        });
        onto1.getClassesInSignature().forEach(owlClass1 -> {
            IRI owl1_iri = owlClass1.getIRI();
            onto2.getClassesInSignature().forEach(owlClass2 -> {
                JaroWinkler jaro = new JaroWinkler();
                double score = jaro.score(owl1_iri.getShortForm(), owlClass2.getIRI().getShortForm());
                intialMapping.add(new MapPair(owl1_iri.toString(), owlClass2.getIRI(), 1 - score));
            });
        });

        onto1.getDataPropertiesInSignature().forEach(owlProp1 -> {
            IRI owl1_iri = owlProp1.getIRI();
            onto2.getDataPropertiesInSignature().forEach(owlProp2 -> {
                IRI owl2_iri = owlProp2.getIRI();
                JaroWinkler jaro = new JaroWinkler();
                double score = jaro.score(owl1_iri.getShortForm(), owlProp2.getIRI().getShortForm());
                intialMapping.add(new MapPair(owl1_iri.toString(), owlProp2.getIRI(), 1 - score));

            });
        });

        onto1.getObjectPropertiesInSignature().forEach(owlProp1 -> {
            IRI owl1_iri = owlProp1.getIRI();
            onto2.getObjectPropertiesInSignature().forEach(owlProp2 -> {
                IRI owl2_iri = owlProp2.getIRI();
                JaroWinkler jaro = new JaroWinkler();
                double score = jaro.score(owl1_iri.getShortForm(), owlProp2.getIRI().getShortForm());
                intialMapping.add(new MapPair(owl1_iri.toString(), owlProp2.getIRI(), 1 - score));

            });
        });
        return intialMapping;
    }
}