import { Fragment, h } from "preact";
import { useState } from "preact/hooks";
import ControlFAB from "../../../components/control-fab";
import Dialog from "../../../components/dialog";
import FAB from "../../../components/fab";
import CenterIcon from "../../../components/icons/center";
import CheckCircleIcon from "../../../components/icons/check-circle";
import HideIcon from "../../../components/icons/hide";
import ShowIcon from "../../../components/icons/show";
import ResolutionCircle from "../../../components/resolution/circle";
import { checkClose, sendMove } from "../../../helpers/api";
import { useAppState } from "../../../helpers/app-state";
import { clauseToString } from "../../../helpers/clause";
import {
    getFOCandidateClauses,
    hideClause,
    showHiddenClauses,
} from "../../../helpers/resolution";
import { Calculus } from "../../../types/app";
import { foExample } from "./example";
import * as style from "./style.scss";

interface Props {}

type SelectedClauses = undefined | [number] | [number, number];

const FOResolutionView: preact.FunctionalComponent<Props> = () => {
    const {
        server,
        [Calculus.foResolution]: cState,
        onError,
        onChange,
        onSuccess,
        smallScreen,
    } = useAppState();

    let state = cState;

    const apiInfo = { onChange, onError, server };

    if (!state) {
        // return <p>Keine Daten vorhanden</p>;
        // Default state for easy testing
        state = foExample;
        onChange(Calculus.foResolution, state);
    }

    const [selectedClauses, setSelectedClauses] = useState<SelectedClauses>(
        undefined,
    );

    const selectedClauseId =
        selectedClauses === undefined ? undefined : selectedClauses[0];

    const showDialog = selectedClauses && selectedClauses.length === 2;

    const candidateClauses = getFOCandidateClauses(
        state.clauseSet,
        state.highlightSelectable,
        selectedClauseId,
    );

    /**
     * The function to call when the user selects a clause
     * @param {number} newClauseId - The id of the clause that was clicked on
     * @returns {void}
     */
    const selectClauseCallback = (newClauseId: number) => {
        if (selectedClauseId === undefined) {
            setSelectedClauses([newClauseId]);
        } else if (newClauseId === selectedClauseId) {
            // The same clause was selected again => reset selection
            setSelectedClauses(undefined);
        } else {
            const candidateClause = candidateClauses.find(
                (c) => c.index === newClauseId,
            )!;
            let resolventLiteral: number | null;
            if (candidateClause.candidateLiterals.length > 1) {
                // Show dialog for literal selection
                setSelectedClauses([selectedClauses![0], newClauseId]);
                return;
            }

            resolventLiteral =
                candidateClause.candidateLiterals.length === 0
                    ? null
                    : candidateClause.candidateLiterals[0];

            // Send resolve move to backend
            if (resolventLiteral !== null) {
                const l1 = getFOCandidateClauses(
                    state!.clauseSet,
                    state!.highlightSelectable,
                    selectedClauseId,
                )[0].index;
                sendMove(
                    server,
                    Calculus.foResolution,
                    state!,
                    {
                        type: "res-resolveunify",
                        c1: selectedClauseId,
                        c2: newClauseId,
                        l1,
                        l2: resolventLiteral,
                    },
                    onChange,
                    onError,
                );
            }
            // Reset selection
            setSelectedClauses(undefined);
        }
    };

    return (
        <Fragment>
            <h2>Resolution View</h2>
            <ResolutionCircle
                clauses={candidateClauses}
                selectClauseCallback={selectClauseCallback}
                selectedClauseId={selectedClauseId}
                highlightSelectable={state.highlightSelectable}
                newestNode={state.newestNode}
            />
            <ControlFAB alwaysOpen={!smallScreen}>
                {selectedClauseId !== undefined ? (
                    <FAB
                        mini={true}
                        extended={true}
                        label="Hide clause"
                        showIconAtEnd={true}
                        icon={<HideIcon />}
                        onClick={() => {
                            hideClause(
                                selectedClauseId,
                                Calculus.foResolution,
                                {
                                    ...apiInfo,
                                    state,
                                },
                            );
                            setSelectedClauses(undefined);
                        }}
                    />
                ) : (
                    undefined
                )}
                {state!.hiddenClauses.clauses.length > 0 ? (
                    <FAB
                        mini={true}
                        extended={true}
                        label="Show all"
                        showIconAtEnd={true}
                        icon={<ShowIcon />}
                        onClick={() => {
                            showHiddenClauses(Calculus.foResolution, {
                                ...apiInfo,
                                state,
                            });
                            setSelectedClauses(undefined);
                        }}
                    />
                ) : undefined}
                <FAB
                    mini={true}
                    extended={true}
                    label="Center"
                    showIconAtEnd={true}
                    icon={<CenterIcon />}
                    onClick={() => dispatchEvent(new CustomEvent("center"))}
                />
                <FAB
                    icon={<CheckCircleIcon />}
                    label="Check"
                    mini={true}
                    extended={true}
                    showIconAtEnd={true}
                    onClick={() =>
                        checkClose(
                            server,
                            onError,
                            onSuccess,
                            Calculus.foResolution,
                            state,
                        )
                    }
                />
            </ControlFAB>
            <Dialog
                open={showDialog}
                label="Choose Literal"
                onClose={() => setSelectedClauses([selectedClauses![0]])}
            >
                {selectedClauses &&
                    selectedClauses.length === 2 &&
                    candidateClauses[selectedClauses[1]].candidateLiterals.map(
                        (l) => (
                            <p
                                class={style.listItem}
                                onClick={() => {
                                    sendMove(
                                        server,
                                        Calculus.foResolution,
                                        state!,
                                        {
                                            type: "res-resolveunify",
                                            c1: selectedClauseId!,
                                            c2: selectedClauses[1],
                                            l1: l,
                                            l2: getFOCandidateClauses(
                                                state!.clauseSet,
                                                state!.highlightSelectable,
                                                selectedClauses[1],
                                            )[0].index,
                                        },
                                        onChange,
                                        onError,
                                    );
                                    setSelectedClauses(undefined);
                                }}
                            >
                                {clauseToString(candidateClauses[l].clause)}
                            </p>
                        ),
                    )}
            </Dialog>
        </Fragment>
    );
};

export default FOResolutionView;
