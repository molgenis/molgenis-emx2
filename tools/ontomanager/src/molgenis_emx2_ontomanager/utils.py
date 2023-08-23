import random

alphabet_cap = list(map(chr, range(ord('A'), ord('Z')+1)))
alphabet_low = list(map(chr, range(ord('a'), ord('z')+1)))


def generate_action_id(action: bool):
    """
    Generate an identification number for logging Ontomanager actions.
    :param action: specifies whether an id for an action is requested.
                   If false, an id for a collection of actions is generated.
    """
    if action:
        str_len = 6
        pop: list = alphabet_low + list(map(str, range(10)))
    else:
        str_len = 4
        pop: list = alphabet_cap + list(map(str, range(10)))

    return "".join(random.choices(population=pop, k=str_len))
