(ns metadactyl.routes.domain.pipeline
  (:use [ring.swagger.schema :only [describe]]
   [schema.core :only [defschema optional-key Any]])
  (:import [java.util UUID]))

(defschema PipelineMapping
  {:target_step (describe UUID "A UUID that is used to identify the Target Step")
   :source_step (describe UUID "A UUID that is used to identify the Source Step")
   ;; KLUDGE
   :map (describe Any "The {'input-uuid': 'output-uuid'} mapping")})

(defschema PipelineStep
  {:id          (describe UUID "A UUID that is used to identify the Step")
   :name        (describe String "The Step's name")
   :description (describe String "The Step's description")
   :task_id     (describe UUID "A UUID that is used to identify this Step's Task")
   :app_type    (describe String "The Step's App type")})

(defschema PipelineApp
  {:id          (describe UUID "A UUID that is used to identify the App")
   :name        (describe String "The App's name")
   :description (describe String "The App's description")
   :steps       (describe [PipelineStep] "The App's steps")

   ;; KLUDGE
   :mappings
   (describe [PipelineMapping]
     "The App's input/output mappings. <b>Note</b>: These objects have a required `map` key with an
      `{'input-uuid': 'output-uuid', ...}` value, but the current version of the documentation
      library does not support documenting this kind of map.")})

(defschema TaskInputOutput
  {:id                   (describe UUID "A UUID that is used to identify the Parameter")
   :name                 (describe String "The Parameter's name")
   :description          (describe String "The Parameter's description")
   :label                (describe String "The Parameter's label")
   (optional-key :value) (describe String "The Output Parameter's value.")
   :format               (describe String "The Parameter's file format.")
   :required             (describe Boolean "Whether or not a value is required for this Parameter.")})

(defschema PipelineTask
  {:id          (describe UUID "A UUID that is used to identify the Task")
   :name        (describe String "The Task's name")
   :description (describe String "The Task's description")
   :inputs      (describe [TaskInputOutput] "The Task's input parameters")
   :outputs     (describe [TaskInputOutput] "The Task's output parameters")})

(defschema Pipeline
  {:apps  (describe [PipelineApp] "The Pipeline App descriptions")
   :tasks (describe [PipelineTask] "The Pipeline's tasks")})
