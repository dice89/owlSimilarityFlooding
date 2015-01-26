package de.unima.dws.alex.onto2graph;


import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.w3c.rdf.model.*;
import org.w3c.rdf.util.RDFFactory;
import org.w3c.rdf.util.RDFFactoryImpl;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mueller on 26/01/15.
 */
public class Onto2GraphConverter {

    //init stuff
    private static  RDFFactory rf = new RDFFactoryImpl();
    private static  NodeFactory nf = rf.getNodeFactory();

    //URI of labels
    private static final String LABEL_SUP_CLASS_PROPERTY = "http://supClass";
    private static final String LABEL_CLASS_ON_PROPERTY ="http://onProperty";
    private static final String LABEL_SUB_PROPERTY ="http://subProperty";
    private static final String LABEL_DOMAIN ="http://domain";
    private static final String LABEL_RANGE ="http://range";

    public  static Model convertToGraph( OWLOntology onto) {

        Model graph = rf.createModel();
        //map all entities to rdf resource
        Map<String, Resource> entity_to_rdf_resource = new HashMap<String,Resource>();
        onto.getClassesInSignature().forEach(owl_class->{
            String iri =  owl_class.getIRI().toString();
            Resource node = null;
            try {
                node = nf.createResource(iri);
                entity_to_rdf_resource.put(iri, node);
            } catch (ModelException e) {
                e.printStackTrace();
            }
        });

        //add all data properties
        onto.getDataPropertiesInSignature().forEach(owl_data_property->{
            String iri =  owl_data_property.getIRI().toString();
            Resource node = null;
            try {
                node = nf.createResource(iri);
                entity_to_rdf_resource.put(iri, node);
            } catch (ModelException e) {
                e.printStackTrace();
            }
        });
        //add all object properties
        onto.getObjectPropertiesInSignature().forEach(owl_obj_property->{
            String iri =  owl_obj_property.getIRI().toString();
            Resource node = null;
            try {
                node = nf.createResource(iri);
                entity_to_rdf_resource.put(iri, node);
            } catch (ModelException e) {
                e.printStackTrace();
            }
        });

        //map all dtproperties
        //map all oproperties

        //get classes and sup Class relation and OnProperty Relation
        onto.getClassesInSignature().stream().forEach(owlClass -> {
            //rdf class
            Resource resource_class = entity_to_rdf_resource.get(owlClass.getIRI().toString());

            //prepare class struct
            onto.getSubClassAxiomsForSubClass(owlClass).forEach(class_axiom-> {
                if(!class_axiom.getSuperClass().isAnonymous()){
                    //consider maybe recursion
                    class_axiom.getSuperClass().asConjunctSet().forEach(super_class -> {
                        try {
                            //add class to resource
                            Resource sup_resource = createResource(super_class.asOWLClass().getIRI().toString());
                            graph.add(createEdge(resource_class,sup_resource,LABEL_SUP_CLASS_PROPERTY));
                        } catch (ModelException e) {
                            e.printStackTrace();
                        }
                    });

                }else{
                    //On Property Restriction
                    class_axiom.getAxiomWithoutAnnotations().getDataPropertiesInSignature().forEach(on_property->{
                        try {
                            Resource res_prob = createResource(on_property.getIRI().toString());

                            graph.add(createEdge(resource_class,res_prob,LABEL_CLASS_ON_PROPERTY));
                        } catch (ModelException e) {
                            e.printStackTrace();
                        }
                    });

                    class_axiom.getAxiomWithoutAnnotations().getObjectPropertiesInSignature().forEach(on_property->{
                        try {
                            Resource res_prob = createResource(on_property.getIRI().toString());

                            graph.add(createEdge(resource_class,res_prob,LABEL_CLASS_ON_PROPERTY));
                        } catch (ModelException e) {
                            e.printStackTrace();
                        }
                    });

                }

            });

        });

        //get Data Property Node and Edges
        onto.getDataPropertiesInSignature().forEach(owl_property-> {

            Resource resource_prop = entity_to_rdf_resource.get(owl_property.getIRI().toString());

            //hiearchy
            onto.getDataSubPropertyAxiomsForSubProperty(owl_property).forEach(property_axiom ->{
                try {
                    Resource sup_res_pro = createResource(property_axiom.getSuperProperty().asOWLDataProperty().getIRI());
                    graph.add(createEdge(resource_prop, sup_res_pro, LABEL_SUB_PROPERTY));
                } catch (ModelException e) {
                    e.printStackTrace();
                }
            });

            //domain
            onto.getDataPropertyDomainAxioms(owl_property).forEach(domain_axiom->{
                domain_axiom.getClassesInSignature().forEach(class_in_domain-> {
                    try {
                        Resource domain_class = createResource(class_in_domain.toStringID());
                        graph.add(createEdge(resource_prop,domain_class,LABEL_DOMAIN));
                    } catch (ModelException e) {
                        e.printStackTrace();
                    }
                });
            });

            //range
            onto.getDataPropertyRangeAxioms(owl_property).forEach(range_axiom->{
                range_axiom.getDatatypesInSignature().forEach(data_type_in_range -> {
                    try {
                        Resource range_class = createResource(data_type_in_range.toStringID());
                        graph.add(createEdge(resource_prop,range_class,LABEL_RANGE));
                    } catch (ModelException e) {
                        e.printStackTrace();
                    }
                });
            });
        });


        //Get Object Property Domain and Range Node and Edges
        onto.getObjectPropertiesInSignature().forEach(owl_property-> {

            Resource resource_prop = entity_to_rdf_resource.get(owl_property.getIRI().toString());

            // Object prop Hierachy
            onto.getObjectSubPropertyAxiomsForSuperProperty(owl_property).forEach(property_axiom -> {
                try {
                    Resource sup_res_pro = createResource(property_axiom.getSuperProperty().asOWLObjectProperty().getIRI());
                    graph.add(createEdge(resource_prop, sup_res_pro, LABEL_SUB_PROPERTY));
                } catch (ModelException e) {
                    e.printStackTrace();
                }

            });

            //Object prop Domain
            onto.getObjectPropertyDomainAxioms(owl_property).forEach(domain_axiom-> {
                domain_axiom.getClassesInSignature().forEach(class_in_domain -> {
                    try {
                        Resource domain_class = createResource(class_in_domain.toStringID());
                        graph.add(createEdge(resource_prop,domain_class,LABEL_DOMAIN));
                    } catch (ModelException e) {
                        e.printStackTrace();
                    }
                });
            });

            //Object RANGE
            onto.getObjectPropertyRangeAxioms(owl_property).forEach(range_axiom->{
                  range_axiom.getClassesInSignature().forEach(class_in_range -> {
                      try {
                          Resource range_class = createResource(class_in_range.toStringID());
                          graph.add(createEdge(resource_prop, range_class, LABEL_RANGE));
                      } catch (ModelException e) {
                          e.printStackTrace();
                      }
                  });
            });
          });

        return graph;
    }

    private static Statement createEdge(Resource a, Resource b, Resource label) throws ModelException {
        return nf.createStatement(a,label,b);
    }

    private static Statement createEdge(Resource a, Resource b, String label) throws ModelException {
        return nf.createStatement(a,nf.createResource(label),b);
    }

    private static Resource createResource(String uri) throws ModelException {
        return nf.createResource(uri);
    }
    private static Resource createResource(IRI iri) throws ModelException {
        return nf.createResource(iri.toString());
    }

}