defmodule PerfCase do
  def data do
    [
      %StructA{
        name: "Item One",
        kind: :alpha,
        options: %StructB{
          fields: [
            %StructC{field: "field_a", source: :auto, order: 1},
            %StructC{field: "field_b", source: :auto, order: 2},
            %StructC{field: "field_c", source: :manual, order: 3},
            %StructC{field: "field_d", source: :manual, order: 4},
            %StructC{field: "field_e", source: :auto, order: 5}
          ]
        },
        listeners: [
          %StructD{name: "Listener A", event_kind: :meeting},
          %StructD{name: "Listener B", event_kind: :call},
          %StructD{name: "Listener C", event_kind: :meeting}
        ]
      },
      %StructA{
        name: "Item Two",
        kind: :beta,
        options: %StructB{
          fields: [
            %StructC{field: "field_f", source: :auto, order: 1},
            %StructC{field: "field_g", source: :auto, order: 2},
            %StructC{field: "field_h", source: :manual, order: 3},
            %StructC{field: "field_i", source: :manual, order: 4},
            %StructC{field: "field_j", source: :auto, order: 5}
          ]
        },
        listeners: [
          %StructD{name: "Listener D", event_kind: :meeting},
          %StructD{name: "Listener E", event_kind: :call},
          %StructD{name: "Listener F", event_kind: :meeting}
        ]
      },
      %StructA{
        name: "Item Three",
        kind: :gamma,
        options: %StructB{
          fields: [
            %StructC{field: "field_k", source: :auto, order: 1},
            %StructC{field: "field_l", source: :auto, order: 2},
            %StructC{field: "field_m", source: :manual, order: 3},
            %StructC{field: "field_n", source: :manual, order: 4},
            %StructC{field: "field_o", source: :auto, order: 5}
          ]
        },
        listeners: [
          %StructD{name: "Listener G", event_kind: :meeting},
          %StructD{name: "Listener H", event_kind: :call},
          %StructD{name: "Listener I", event_kind: :meeting}
        ]
      },
      %StructA{
        name: "Item Four",
        kind: :delta,
        options: %StructB{
          fields: [
            %StructC{field: "field_p", source: :auto, order: 1},
            %StructC{field: "field_q", source: :auto, order: 2},
            %StructC{field: "field_r", source: :manual, order: 3},
            %StructC{field: "field_s", source: :manual, order: 4},
            %StructC{field: "field_t", source: :auto, order: 5}
          ]
        },
        listeners: [
          %StructD{name: "Listener J", event_kind: :meeting},
          %StructD{name: "Listener K", event_kind: :call},
          %StructD{name: "Listener L", event_kind: :meeting}
        ]
      },
      %StructA{
        name: "Item Five",
        kind: :epsilon,
        options: %StructB{
          fields: [
            %StructC{field: "field_u", source: :auto, order: 1},
            %StructC{field: "field_v", source: :auto, order: 2},
            %StructC{field: "field_w", source: :manual, order: 3},
            %StructC{field: "field_x", source: :manual, order: 4},
            %StructC{field: "field_y", source: :auto, order: 5}
          ]
        },
        listeners: [
          %StructD{name: "Listener M", event_kind: :meeting},
          %StructD{name: "Listener N", event_kind: :call},
          %StructD{name: "Listener O", event_kind: :meeting}
        ]
      },
      %StructA{
        name: "Item Six",
        kind: :zeta,
        options: %StructB{
          fields: [
            %StructC{field: "field_z", source: :auto, order: 1},
            %StructC{field: "field_aa", source: :auto, order: 2},
            %StructC{field: "field_ab", source: :manual, order: 3},
            %StructC{field: "field_ac", source: :manual, order: 4},
            %StructC{field: "field_ad", source: :auto, order: 5}
          ]
        },
        listeners: [
          %StructD{name: "Listener P", event_kind: :meeting},
          %StructD{name: "Listener Q", event_kind: :call},
          %StructD{name: "Listener R", event_kind: :meeting}
        ]
      },
      %{
        name: "Simple One",
        kind: :simple,
        listeners: [
          %{name: "Listener S", event_kind: :call},
          %{name: "Listener T", event_kind: :meeting},
          %{name: "Listener U", event_kind: :call}
        ]
      },
      %{
        name: "Simple Two",
        kind: :simple,
        listeners: [
          %{name: "Listener V", event_kind: :call},
          %{name: "Listener W", event_kind: :meeting},
          %{name: "Listener X", event_kind: :call}
        ]
      }
    ]
  end
end
