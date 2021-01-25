import { Fragment, h } from "preact";
import { Calculus, PropCalculusType, PSCCalculusType } from "../../../types/calculus";
import { useCallback, useEffect, useState } from "preact/hooks";
import OptionList from "../../../components/input/option-list";
import { DragTransform } from "../../../types/ui";
import { useAppState } from "../../../util/app-state";
import { ruleSetToStringArray } from "../../../util/rule";
import { stringArrayToStringMap } from "../../../util/array-to-map";
import { getRuleSet } from "../../../types/calculus/rules";
import PSCTreeView from "../../../components/calculus/psc/tree"
import { FormulaTreeLayoutNode, PSCNode, PSCTreeLayoutNode } from "../../../types/calculus/psc";

import * as style from "./style.scss";
import { route } from "preact-router";
import { selected } from "../../../components/svg/rectangle/style.scss";
import PSCFAB from "../../../components/calculus/psc/fab";
import { NotificationType } from "../../../types/app/notification";
import TutorialDialog from "../../../components/tutorial/dialog";
import { sendMove } from "../../../util/api";
import Dialog from "../../../components/dialog";


interface Props {
    calculus: PSCCalculusType;
}

const PSCView: preact.FunctionalComponent<Props> = ({calculus}) => {
    
    const {
        server,
        [calculus]: cState,
        smallScreen,
        notificationHandler,
        onChange,
    } = useAppState();

    let selectedRule: string = "";

    const state = cState;
    if (!state) {
        route(`/${calculus}`);
        return null;
    }
    
    const [selectedNodeId, setSelectedNodeId] = useState<number | undefined>(
         undefined
    );

    const [selectedListIndex, setSelectedListIndex] = useState<string | undefined>(
        undefined
    );

    const selectedNode =
         selectedNodeId !== undefined ? state.tree[selectedNodeId] : undefined;

    const ruleOptions = stringArrayToStringMap(ruleSetToStringArray(getRuleSet()));

    const [selectedRuleId, setSelectedRuleId] = useState<
        number | undefined
    >(undefined);

    const [showRuleDialog, setShowRuleDialog] = useState(false);

    const selectRuleCallback = (newRuleId: number) => {
        if(newRuleId === selectedRuleId){
            // The same Rule was selected again => deselect it
            setSelectedRuleId(undefined);
        }else{
            setSelectedRuleId(newRuleId);
            if(newRuleId !== undefined && selectedNode !== undefined && selectedNode.type === "leaf" && selectedListIndex !== undefined){
                if(newRuleId === 0){
                    sendMove(
                        server, calculus, state, {type: "Ax", nodeID: selectedNodeId!}, onChange,notificationHandler,
                    )
                } else {
                    sendMove(
                        server, calculus, state, {type: ruleOptions.get(newRuleId)!, nodeID: selectedNodeId!, listIndex: parseStringToListIndex(selectedListIndex)}, onChange,notificationHandler,
                    )
                }
                setSelectedNodeId(undefined);
                setSelectedRuleId(undefined);
                
            }
        }
    };

    const parseStringToListIndex = (str: string) => {
        return parseInt(str.substring(1));
    }

    const selectNodeCallback = (
        newNode: PSCTreeLayoutNode,
        selectValue?: boolean,
    ) => {
        console.log("seleect Node");
        const newNodeIsLeaf = newNode.type === "leaf";

        if (selectValue === undefined) {
            if(newNode.id === selectedNodeId){
                setSelectedNodeId(undefined);
            }else {
                setSelectedNodeId(newNode.id);
                if(selectedRuleId !== undefined && newNodeIsLeaf && selectedListIndex !== undefined){
                    if(selectedRuleId === 0){
                        sendMove(
                            server, calculus, state, {type: "Ax", nodeID: newNode.id}, onChange,notificationHandler,
                        )
                    } else {
                        sendMove(
                            server, calculus, state, {type: ruleOptions.get(selectedRuleId)!, nodeID: newNode.id, listIndex: parseStringToListIndex(selectedListIndex)}, onChange,notificationHandler,
                        )
                    }
                    setSelectedNodeId(undefined);
                    setSelectedRuleId(undefined);
                    
                }
            }
        }else{
            if (selectValue === true) {
                setSelectedNodeId(newNode.id);
            } else {
                setSelectedNodeId(undefined);
            }
        }
        
    };

    const selectFormulaCallback = (
        newFormula: FormulaTreeLayoutNode,
    ) => {
        console.log("seleaked Formula");
        event?.stopPropagation();
        if(newFormula.id === selectedListIndex){
            setSelectedListIndex(undefined);
            setSelectedNodeId(undefined);
        } else {
            setSelectedListIndex(newFormula.id);
        }
        
    }
    
    
    return (
        <Fragment>
            <h2>Sequent Calculus View</h2>

            <div class={style.view}>
                {!smallScreen && (
                    <div>
                        <OptionList
                            options={ruleOptions}
                            selectedOptionIds={
                                selectedRuleId !== undefined
                                    ? [selectedRuleId]
                                    : undefined
                            }
                            selectOptionCallback={(keyValuePair) =>
                                selectRuleCallback(keyValuePair[0])
                            }
                        />
                    </div>
                )}

                <PSCTreeView
                    nodes={state.tree}
                    smallScreen={smallScreen}
                    selectedNodeId={selectedNodeId}
                    selectedRuleName={selectedRule}
                    selectNodeCallback={selectNodeCallback}
                    selectFormulaCallback={selectFormulaCallback}
                    selectedListIndex={selectedListIndex}
                />
            </div>

            <Dialog
                open={showRuleDialog}
                label="Choose Rule"
                onClose={() => setShowRuleDialog(false)}
            >
                <OptionList
                    options={ruleOptions}
                    selectOptionCallback={(keyValuePair) => {
                        setShowRuleDialog(false);
                        selectRuleCallback(keyValuePair[0]);
                    }}
                />
            </Dialog>


            <PSCFAB 
                calculus={Calculus.psc}
                state={state}
                selectedNodeId={selectedNodeId}
                ruleCallback={ () => setShowRuleDialog(true)}
            />
            <TutorialDialog calculus={Calculus.psc} />

        </Fragment>

    );
    

};

export default PSCView;